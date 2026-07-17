package com.nexusxva.closechecklist.application;

import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.backoffice.application.BackOfficeOperationsReportService;
import com.nexusxva.closechecklist.domain.CloseChecklistRun;
import com.nexusxva.closechecklist.domain.CloseChecklistRunStatus;
import com.nexusxva.closechecklist.domain.CloseChecklistStepStatus;
import com.nexusxva.cva.application.CvaCalculationCommand;
import com.nexusxva.cva.application.CvaCalculationService;
import com.nexusxva.eod.application.PortfolioDailyPnlService;
import com.nexusxva.eod.application.PortfolioEodService;
import com.nexusxva.exposure.application.ExposureSimulationCommand;
import com.nexusxva.exposure.application.ExposureSimulationService;
import com.nexusxva.executescript.application.ExecuteScriptService;
import com.nexusxva.executescript.application.RunExecuteScriptCommand;
import com.nexusxva.operationalcontrol.application.OperationalControlStore;
import com.nexusxva.operationalcontrol.domain.CloseChecklistPhase;
import com.nexusxva.operationalcontrol.domain.CloseChecklistSettings;
import com.nexusxva.operationalcontrol.domain.CloseChecklistStepDefinition;
import com.nexusxva.operationalcontrol.domain.CloseChecklistStepType;
import com.nexusxva.operationalcontrol.domain.OperationalControlSettings;
import com.nexusxva.portfolio.application.PortfolioBlackScholesPricingService;
import com.nexusxva.portfolio.application.PortfolioService;
import com.nexusxva.reporting.application.ReportSnapshotService;
import com.nexusxva.reporting.domain.ReportSnapshotScopeType;
import com.nexusxva.reporting.domain.ReportSnapshotType;
import com.nexusxva.shared.error.ResourceNotFoundException;
import com.nexusxva.tradelifecycle.application.TradeLifecycleService;
import com.nexusxva.valuationruns.application.ValuationRunService;
import com.nexusxva.valuationruns.domain.ValuationRunType;
import com.nexusxva.xva.application.CvaCurveMasterDataService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CloseChecklistService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloseChecklistService.class);

    private final OperationalControlStore operationalControlStore;
    private final CloseChecklistStore store;
    private final PortfolioService portfolioService;
    private final PortfolioBlackScholesPricingService pricingService;
    private final ExposureSimulationService exposureService;
    private final CvaCalculationService cvaService;
    private final CvaCurveMasterDataService curveMasterDataService;
    private final PortfolioEodService eodService;
    private final PortfolioDailyPnlService pnlService;
    private final BackOfficeOperationsReportService operationsReportService;
    private final TradeLifecycleService lifecycleService;
    private final ReportSnapshotService reportSnapshotService;
    private final ValuationRunService valuationRunService;
    private final ExecuteScriptService executeScriptService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public CloseChecklistService(
            OperationalControlStore operationalControlStore,
            CloseChecklistStore store,
            PortfolioService portfolioService,
            PortfolioBlackScholesPricingService pricingService,
            ExposureSimulationService exposureService,
            CvaCalculationService cvaService,
            CvaCurveMasterDataService curveMasterDataService,
            PortfolioEodService eodService,
            PortfolioDailyPnlService pnlService,
            BackOfficeOperationsReportService operationsReportService,
            TradeLifecycleService lifecycleService,
            ReportSnapshotService reportSnapshotService,
            ValuationRunService valuationRunService,
            ExecuteScriptService executeScriptService,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper
    ) {
        this.operationalControlStore = operationalControlStore;
        this.store = store;
        this.portfolioService = portfolioService;
        this.pricingService = pricingService;
        this.exposureService = exposureService;
        this.cvaService = cvaService;
        this.curveMasterDataService = curveMasterDataService;
        this.eodService = eodService;
        this.pnlService = pnlService;
        this.operationsReportService = operationsReportService;
        this.lifecycleService = lifecycleService;
        this.reportSnapshotService = reportSnapshotService;
        this.valuationRunService = valuationRunService;
        this.executeScriptService = executeScriptService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CloseChecklistRun runManual(LocalDate businessDate, AuthSession session) {
        OperationalControlSettings settings = operationalControlStore.settings();
        LocalDate resolvedDate = businessDate == null
                ? LocalDate.now(settings.timezone())
                : businessDate;
        UUID runId = UUID.randomUUID();
        store.startRun(runId, resolvedDate, "MANUAL_BO", userId(session), settings.closeChecklist(), Instant.now());
        execute(runId, resolvedDate, "MANUAL_BO", settings.closeChecklist(), session);
        return get(runId);
    }

    @Transactional
    public boolean runScheduledIfDue(LocalDate businessDate) {
        OperationalControlSettings settings = operationalControlStore.settings();
        CloseChecklistSettings checklist = settings.closeChecklist();
        if (!checklist.enabled()) {
            return false;
        }
        UUID runId = UUID.randomUUID();
        if (!store.tryStartScheduledRun(runId, businessDate, checklist, Instant.now())) {
            return true;
        }
        execute(runId, businessDate, "SCHEDULED", checklist, null);
        return true;
    }

    @Transactional(readOnly = true)
    public CloseChecklistRun get(UUID runId) {
        return store.find(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Close checklist run not found"));
    }

    @Transactional(readOnly = true)
    public List<CloseChecklistRun> recent(int limit) {
        return store.recent(limit);
    }

    @Transactional(readOnly = true)
    public boolean scheduledAlreadyRan(LocalDate businessDate) {
        return store.latestScheduledBusinessDate()
                .map(date -> !date.isBefore(businessDate))
                .orElse(false);
    }

    private void execute(UUID runId, LocalDate businessDate, String source, CloseChecklistSettings checklist, AuthSession session) {
        LOGGER.info("Close checklist started runId={} businessDate={} source={}", runId, businessDate, source);
        boolean hasFailure = false;
        boolean blocked = false;
        List<CloseChecklistStepDefinition> steps = checklist.enabledSteps();
        for (CloseChecklistStepDefinition definition : steps) {
            UUID stepId = UUID.randomUUID();
            store.createStep(stepId, runId, definition);
            if (blocked) {
                store.skipStep(stepId, "Skipped because a critical PRE_EOD step failed", Map.of());
                continue;
            }
            store.markStepRunning(stepId, Instant.now());
            try {
                Object output = executeStep(definition, businessDate, checklist, session, source);
                store.completeStep(stepId, "Step completed", output);
            } catch (RuntimeException exception) {
                hasFailure = true;
                String message = sanitizedMessage(exception);
                store.failStep(stepId, message, Map.of("error", message));
                LOGGER.warn("Close checklist step failed runId={} stepType={} businessDate={} reason={}",
                        runId, definition.stepType(), businessDate, message);
                if (definition.phase() == CloseChecklistPhase.PRE_EOD && definition.critical()) {
                    blocked = true;
                }
            }
        }
        CloseChecklistRunStatus status = blocked
                ? CloseChecklistRunStatus.FAILED
                : hasFailure ? CloseChecklistRunStatus.PARTIAL : CloseChecklistRunStatus.COMPLETED;
        store.completeRun(runId, status, status == CloseChecklistRunStatus.COMPLETED ? "Close checklist completed" : "Close checklist completed with issues");
        LOGGER.info("Close checklist completed runId={} businessDate={} status={}", runId, businessDate, status);
    }

    private Object executeStep(
            CloseChecklistStepDefinition definition,
            LocalDate businessDate,
            CloseChecklistSettings checklist,
            AuthSession session,
            String source
    ) {
        return switch (definition.stepType()) {
            case BO_OPERATIONS_REPORT -> runBoOperationsReport(session, businessDate, source);
            case BO_LIFECYCLE_REPORT -> runBoLifecycleReport(session, businessDate, source);
            case FO_PNL_REPORT -> runFoPnlReport(session, businessDate, checklist);
            case PORTFOLIO_PRICING -> runPricing(session, businessDate, checklist);
            case EXPOSURE -> runExposure(session, businessDate, checklist);
            case CVA -> runCva(session, businessDate, checklist);
            case EOD -> runEod(businessDate, checklist, source);
            case SCRIPT_TEMPLATE -> runScriptTemplate(definition, businessDate, checklist, session);
        };
    }

    private Object runScriptTemplate(
            CloseChecklistStepDefinition definition,
            LocalDate businessDate,
            CloseChecklistSettings checklist,
            AuthSession session
    ) {
        var template = executeScriptService.getTemplate(definition.templateId());
        boolean containsEodCapture = template.steps().stream()
                .anyMatch(step -> step.enabled() && step.stepType() == com.nexusxva.executescript.domain.ExecuteScriptStepType.EOD_CAPTURE);
        if (containsEodCapture) {
            throw new IllegalArgumentException("Close checklist templates cannot contain EOD_CAPTURE");
        }
        var parameters = objectMapper.valueToTree(Map.of(
                "valuationDate", businessDate.toString(),
                "exposure", Map.of(
                        "horizonDays", checklist.riskDefaults().horizonDays(),
                        "timeSteps", checklist.riskDefaults().timeSteps(),
                        "paths", checklist.riskDefaults().paths(),
                        "seed", checklist.riskDefaults().seed(),
                        "pfeConfidenceLevel", checklist.riskDefaults().pfeConfidenceLevel()
                ),
                "cva", cvaDefaults(checklist)
        ));
        var run = executeScriptService.run(new RunExecuteScriptCommand(
                definition.templateId(),
                definition.scriptMode(),
                businessDate,
                checklist.portfolioIds(),
                parameters
        ), session);
        if (run.status() == com.nexusxva.executescript.domain.ExecuteScriptRunStatus.FAILED) {
            throw new IllegalArgumentException("ExecuteScript failed: " + run.message());
        }
        return Map.of("executeScriptRunId", run.id(), "templateId", definition.templateId(), "status", run.status());
    }

    private Map<String, Object> cvaDefaults(CloseChecklistSettings checklist) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("lossGivenDefault", checklist.riskDefaults().lossGivenDefault());
        values.put("creditCurveId", checklist.riskDefaults().creditCurveId());
        values.put("discountCurveId", checklist.riskDefaults().discountCurveId());
        return values;
    }

    private Object runBoOperationsReport(AuthSession session, LocalDate businessDate, String source) {
        var report = operationsReportService.report();
        var snapshot = reportSnapshotService.record(
                session,
                ReportSnapshotType.BO_OPERATIONS,
                "BO Operations Reporting - Close Checklist",
                businessDate,
                ReportSnapshotScopeType.BACK_OFFICE,
                null,
                "Back Office",
                Map.of("source", source),
                report,
                Map.of("portfolios", report.portfolios(), "failedPnlPortfolios", report.failedPnlPortfolios())
        );
        return Map.of("reportSnapshotId", snapshot.id(), "portfolios", report.portfolios());
    }

    private Object runBoLifecycleReport(AuthSession session, LocalDate businessDate, String source) {
        var report = lifecycleService.reportForBackOffice();
        var snapshot = reportSnapshotService.record(
                session,
                ReportSnapshotType.BO_LIFECYCLE,
                "BO Lifecycle Reporting - Close Checklist",
                businessDate,
                ReportSnapshotScopeType.BACK_OFFICE,
                null,
                "Back Office",
                Map.of("source", source),
                report,
                Map.of("total", report.total(), "pendingValidation", report.pendingValidation())
        );
        return Map.of("reportSnapshotId", snapshot.id(), "pendingValidation", report.pendingValidation());
    }

    private Object runFoPnlReport(AuthSession session, LocalDate businessDate, CloseChecklistSettings checklist) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (UUID portfolioId : checklist.portfolioIds()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("portfolioId", portfolioId);
            try {
                var pnl = pnlService.calculate(portfolioId, businessDate);
                row.put("status", "OK");
                row.put("dailyPnl", pnl.dailyPnl());
                row.put("sinceTradePnl", pnl.sinceTradePnl());
                row.put("currentMarketValue", pnl.currentMarketValue());
            } catch (RuntimeException exception) {
                row.put("status", "FAILED");
                row.put("error", sanitizedMessage(exception));
            }
            rows.add(row);
        }
        var snapshot = reportSnapshotService.record(
                session,
                ReportSnapshotType.FO_PNL_SNAPSHOT,
                "FO P&L Snapshot - Close Checklist",
                businessDate,
                ReportSnapshotScopeType.GLOBAL,
                null,
                "Close Checklist",
                Map.of("portfolioIds", checklist.portfolioIds()),
                Map.of("businessDate", businessDate, "portfolios", rows),
                Map.of("portfolioCount", rows.size())
        );
        return Map.of("reportSnapshotId", snapshot.id(), "portfolioCount", rows.size());
    }

    private Object runPricing(AuthSession session, LocalDate businessDate, CloseChecklistSettings checklist) {
        List<Map<String, Object>> outputs = new ArrayList<>();
        for (UUID portfolioId : checklist.portfolioIds()) {
            try {
                var result = pricingService.price(portfolioId, businessDate);
                var run = valuationRunService.recordSuccess(
                        session,
                        portfolioId,
                        ValuationRunType.PRICING,
                        result.model(),
                        result.valuationDate(),
                        Map.of("portfolioId", portfolioId, "valuationDate", businessDate, "source", "CLOSE_CHECKLIST"),
                        result,
                        Map.of("totalPrice", result.totalPrice(), "baseCurrency", result.baseCurrency())
                );
                outputs.add(Map.of("portfolioId", portfolioId, "status", "SUCCESS", "valuationRunId", run.id()));
            } catch (RuntimeException exception) {
                var run = valuationRunService.recordFailure(
                        session,
                        portfolioId,
                        ValuationRunType.PRICING,
                        "BLACK_SCHOLES_PORTFOLIO_PRICING_V1",
                        businessDate,
                        Map.of("portfolioId", portfolioId, "valuationDate", businessDate, "source", "CLOSE_CHECKLIST"),
                        exception
                );
                outputs.add(Map.of("portfolioId", portfolioId, "status", "FAILED", "valuationRunId", run.id(), "error", sanitizedMessage(exception)));
            }
        }
        return Map.of("portfolioResults", outputs);
    }

    private Object runExposure(AuthSession session, LocalDate businessDate, CloseChecklistSettings checklist) {
        List<Map<String, Object>> outputs = new ArrayList<>();
        for (UUID portfolioId : checklist.portfolioIds()) {
            var defaults = checklist.riskDefaults();
            var command = new ExposureSimulationCommand(
                    portfolioId,
                    businessDate,
                    defaults.horizonDays(),
                    defaults.timeSteps(),
                    defaults.paths(),
                    defaults.seed(),
                    defaults.pfeConfidenceLevel()
            );
            try {
                var result = exposureService.simulate(command);
                var run = valuationRunService.recordSuccess(
                        session,
                        portfolioId,
                        ValuationRunType.EXPOSURE,
                        result.model(),
                        result.valuationDate(),
                        command,
                        result,
                        Map.of("points", result.points().size(), "paths", result.paths())
                );
                outputs.add(Map.of("portfolioId", portfolioId, "status", "SUCCESS", "valuationRunId", run.id()));
            } catch (RuntimeException exception) {
                var run = valuationRunService.recordFailure(session, portfolioId, ValuationRunType.EXPOSURE, "GBM_BLACK_SCHOLES_EXPOSURE_V1", businessDate, command, exception);
                outputs.add(Map.of("portfolioId", portfolioId, "status", "FAILED", "valuationRunId", run.id(), "error", sanitizedMessage(exception)));
            }
        }
        return Map.of("portfolioResults", outputs);
    }

    private Object runCva(AuthSession session, LocalDate businessDate, CloseChecklistSettings checklist) {
        var defaults = checklist.riskDefaults();
        if (defaults.creditCurveId() == null || defaults.discountCurveId() == null) {
            throw new IllegalArgumentException("Close checklist CVA requires creditCurveId and discountCurveId");
        }
        var creditCurve = curveMasterDataService.activeCreditCurvePoints(defaults.creditCurveId());
        var discountCurve = curveMasterDataService.activeDiscountCurvePoints(defaults.discountCurveId());
        List<Map<String, Object>> outputs = new ArrayList<>();
        for (UUID portfolioId : checklist.portfolioIds()) {
            var command = new CvaCalculationCommand(
                    portfolioId,
                    businessDate,
                    defaults.horizonDays(),
                    defaults.timeSteps(),
                    defaults.paths(),
                    defaults.seed(),
                    defaults.pfeConfidenceLevel(),
                    defaults.lossGivenDefault(),
                    null,
                    null,
                    creditCurve,
                    discountCurve
            );
            try {
                var result = cvaService.calculate(command);
                var run = valuationRunService.recordSuccess(
                        session,
                        portfolioId,
                        ValuationRunType.CVA,
                        result.model(),
                        result.valuationDate(),
                        command,
                        result,
                        Map.of("cva", result.cva(), "creditMethod", result.creditMethod(), "discountMethod", result.discountMethod())
                );
                outputs.add(Map.of("portfolioId", portfolioId, "status", "SUCCESS", "valuationRunId", run.id()));
            } catch (RuntimeException exception) {
                var run = valuationRunService.recordFailure(session, portfolioId, ValuationRunType.CVA, "SIMPLIFIED_CVA_V1", businessDate, command, exception);
                outputs.add(Map.of("portfolioId", portfolioId, "status", "FAILED", "valuationRunId", run.id(), "error", sanitizedMessage(exception)));
            }
        }
        return Map.of("portfolioResults", outputs);
    }

    private Object runEod(LocalDate businessDate, CloseChecklistSettings checklist, String source) {
        List<Map<String, Object>> outputs = new ArrayList<>();
        int captured = 0;
        int failed = 0;
        for (UUID portfolioId : checklist.portfolioIds()) {
            try {
                var snapshot = eodService.capture(portfolioId, businessDate, "CLOSE_CHECKLIST_" + source);
                captured++;
                outputs.add(Map.of("portfolioId", portfolioId, "status", "CAPTURED", "eodRunId", snapshot.id()));
            } catch (RuntimeException exception) {
                failed++;
                outputs.add(Map.of("portfolioId", portfolioId, "status", "FAILED", "error", sanitizedMessage(exception)));
            }
        }
        if (failed > 0) {
            throw new IllegalArgumentException("EOD failed for " + failed + " portfolio(s)");
        }
        return Map.of("captured", captured, "portfolioResults", outputs);
    }

    private UUID userId(AuthSession session) {
        return session == null ? null : session.user().id();
    }

    private String sanitizedMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
