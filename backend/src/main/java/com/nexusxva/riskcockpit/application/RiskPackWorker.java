package com.nexusxva.riskcockpit.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusxva.cva.domain.CvaInput;
import com.nexusxva.cva.domain.SimplifiedCvaCalculator;
import com.nexusxva.exposure.application.ExposureSimulationCommand;
import com.nexusxva.exposure.application.ExposureSimulationResult;
import com.nexusxva.exposure.application.ExposureSimulationService;
import com.nexusxva.frontoffice.application.FrontOfficeStressTestService;
import com.nexusxva.eod.application.PortfolioDailyPnlService;
import com.nexusxva.eod.application.PortfolioDailyPnl;
import com.nexusxva.marketrisk.application.HistoricalVarResult;
import com.nexusxva.marketrisk.application.HistoricalVarService;
import com.nexusxva.operationalcontrol.application.OperationalControlService;
import com.nexusxva.operationalcontrol.domain.CloseChecklistRiskDefaults;
import com.nexusxva.portfolio.application.PortfolioBlackScholesPricingService;
import com.nexusxva.portfolio.application.PortfolioBlackScholesPricingResult;
import com.nexusxva.portfolio.application.PortfolioPositionMarketData;
import com.nexusxva.riskcockpit.domain.*;
import com.nexusxva.xva.application.CvaCurveMasterDataService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RiskPackWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger(RiskPackWorker.class);
    private final RiskPackStore store;
    private final PortfolioBlackScholesPricingService pricing;
    private final PortfolioDailyPnlService pnl;
    private final FrontOfficeStressTestService stress;
    private final HistoricalVarService historicalVar;
    private final ExposureSimulationService exposure;
    private final CvaCurveMasterDataService curves;
    private final OperationalControlService operationalControl;
    private final ObjectMapper mapper;
    private final SimplifiedCvaCalculator cvaCalculator = new SimplifiedCvaCalculator();

    public RiskPackWorker(RiskPackStore store, PortfolioBlackScholesPricingService pricing, PortfolioDailyPnlService pnl,
                          FrontOfficeStressTestService stress, HistoricalVarService historicalVar,
                          ExposureSimulationService exposure, CvaCurveMasterDataService curves,
                          OperationalControlService operationalControl, ObjectMapper mapper) {
        this.store=store; this.pricing=pricing; this.pnl=pnl; this.stress=stress; this.historicalVar=historicalVar;
        this.exposure=exposure; this.curves=curves; this.operationalControl=operationalControl; this.mapper=mapper;
    }

    public void execute(UUID runId) {
        RiskPackRun run = store.find(runId).orElseThrow();
        store.markRunRunning(runId, Instant.now());
        AtomicReference<Instant> marketAsOf = new AtomicReference<>();
        int succeeded=0, failed=0, skipped=0;
        try {
            PricingPackResult pricingResult = componentResult(runId, RiskPackComponentType.PRICING, () -> new PricingPackResult(
                    pricing.price(run.portfolioId(), run.valuationDate()),
                    pnl.calculate(run.portfolioId(), run.valuationDate())));
            if (pricingResult == null) {
                failed++;
            } else {
                succeeded++;
                marketAsOf.set(pricingMarketDataAsOf(pricingResult.valuation()));
            }
            List<FrontOfficeStressTestService.StressScenario> standard = List.of(
                    new FrontOfficeStressTestService.StressScenario("Spot -5%, Vol +250bp", -.05, 250, 0, 0),
                    new FrontOfficeStressTestService.StressScenario("Spot -10%, Vol +500bp", -.10, 500, 0, 0),
                    new FrontOfficeStressTestService.StressScenario("Spot +5%, Vol -100bp", .05, -100, 0, 0),
                    new FrontOfficeStressTestService.StressScenario("Rates +100bp", 0, 0, 100, 0),
                    new FrontOfficeStressTestService.StressScenario("Rates -100bp", 0, 0, -100, 0));
            if (runComponent(runId, RiskPackComponentType.STRESS, () -> stress.run(run.portfolioId(), run.valuationDate(), null, standard))) succeeded++; else failed++;

            HistoricalVarResult varResult = componentResult(runId, RiskPackComponentType.VAR,
                    () -> historicalVar.calculate(run.portfolioId(), run.valuationDate()));
            if (varResult == null) failed++; else { succeeded++; marketAsOf.set(varResult.marketDataAsOf()); }

            CloseChecklistRiskDefaults defaults = operationalControl.settings().closeChecklist().riskDefaults();
            ExposureSimulationResult exposureResult = componentResult(runId, RiskPackComponentType.EXPOSURE, () -> exposure.simulate(
                    new ExposureSimulationCommand(run.portfolioId(), run.valuationDate(), defaults.horizonDays(), defaults.timeSteps(),
                            defaults.paths(), defaults.seed(), defaults.pfeConfidenceLevel())));
            if (exposureResult == null) {
                failed++;
                store.completeComponent(runId, RiskPackComponentType.CVA, RiskPackComponentStatus.SKIPPED, null,
                        "Exposure component failed", Instant.now());
                skipped++;
            } else {
                succeeded++;
                if (defaults.creditCurveId() == null || defaults.discountCurveId() == null) {
                    store.completeComponent(runId, RiskPackComponentType.CVA, RiskPackComponentStatus.SKIPPED, null,
                            "Approved creditCurveId and discountCurveId are required in Operational Control", Instant.now());
                    skipped++;
                } else {
                    boolean cvaOk = runComponent(runId, RiskPackComponentType.CVA, () -> {
                        var credit = curves.activeCreditCurvePoints(defaults.creditCurveId());
                        var discount = curves.activeDiscountCurvePoints(defaults.discountCurveId());
                        var result = cvaCalculator.calculate(new CvaInput(run.valuationDate(), exposureResult.points(),
                                defaults.lossGivenDefault(), null, null, credit, discount));
                        return Map.of("model", "SIMPLIFIED_CVA_V1", "cva", result.cva(), "creditMethod", result.creditMethod(),
                                "discountMethod", result.discountMethod(), "points", result.points(), "exposureModel", exposureResult.model());
                    });
                    if (cvaOk) succeeded++; else failed++;
                }
            }
            RiskPackRunStatus status = failed == 0 && skipped == 0 ? RiskPackRunStatus.SUCCESS
                    : succeeded > 0 ? RiskPackRunStatus.PARTIAL : RiskPackRunStatus.FAILED;
            store.completeRun(runId, status, marketAsOf.get(), status == RiskPackRunStatus.SUCCESS ? null : "One or more components were unavailable", Instant.now());
            LOGGER.info("Risk pack completed runId={} portfolioId={} status={} success={} failed={} skipped={}", runId, run.portfolioId(), status, succeeded, failed, skipped);
        } catch (Exception exception) {
            store.completeRun(runId, RiskPackRunStatus.FAILED, marketAsOf.get(), clean(exception), Instant.now());
            LOGGER.error("Risk pack failed runId={} portfolioId={} reason={}", runId, run.portfolioId(), clean(exception));
        }
    }

    private boolean runComponent(UUID runId, RiskPackComponentType type, ThrowingSupplier<?> supplier) {
        return componentResult(runId, type, supplier) != null;
    }
    private <T> T componentResult(UUID runId, RiskPackComponentType type, ThrowingSupplier<T> supplier) {
        store.markComponentRunning(runId, type, Instant.now());
        try {
            T result=supplier.get();
            store.completeComponent(runId, type, RiskPackComponentStatus.SUCCESS, mapper.valueToTree(result), null, Instant.now());
            return result;
        } catch (Exception exception) {
            store.completeComponent(runId, type, RiskPackComponentStatus.FAILED, null, clean(exception), Instant.now());
            LOGGER.warn("Risk pack component failed runId={} component={} reason={}", runId, type, clean(exception));
            return null;
        }
    }
    private String clean(Exception exception) { String value=exception.getMessage(); return value==null||value.isBlank()?"Component unavailable":value.substring(0,Math.min(500,value.length())); }
    private Instant pricingMarketDataAsOf(PortfolioBlackScholesPricingResult result) {
        return java.util.stream.Stream.concat(
                        result.positions().stream().map(position -> position.marketData()),
                        result.cashEquityPositions().stream().map(position -> position.marketData()))
                .filter(java.util.Objects::nonNull)
                .map(PortfolioPositionMarketData::asOf)
                .filter(java.util.Objects::nonNull)
                .min(Instant::compareTo)
                .orElse(null);
    }
    private record PricingPackResult(PortfolioBlackScholesPricingResult valuation, PortfolioDailyPnl pnl) {}
    @FunctionalInterface private interface ThrowingSupplier<T> { T get() throws Exception; }
}
