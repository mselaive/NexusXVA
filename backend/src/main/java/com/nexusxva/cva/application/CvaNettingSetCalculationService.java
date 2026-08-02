package com.nexusxva.cva.application;

import com.nexusxva.cva.domain.CvaInput;
import com.nexusxva.cva.domain.CvaResult;
import com.nexusxva.cva.domain.SimplifiedCvaCalculator;
import com.nexusxva.exposure.application.ExposureSimulationCommand;
import com.nexusxva.exposure.application.ExposureSimulationResult;
import com.nexusxva.exposure.domain.ExposurePoint;
import com.nexusxva.xva.domain.NettingSet;
import com.nexusxva.xva.domain.NettingSetPortfolio;
import com.nexusxva.xva.application.XvaReferenceDataService;
import com.nexusxva.exposure.application.ExposureSimulationService;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CvaNettingSetCalculationService {

    private static final String MODEL = "SIMPLIFIED_CVA_NETTING_SET_V1";

    private final XvaReferenceDataService xvaReferenceDataService;
    private final ExposureSimulationService exposureSimulationService;
    private final SimplifiedCvaCalculator calculator;

    public CvaNettingSetCalculationService(
            XvaReferenceDataService xvaReferenceDataService,
            ExposureSimulationService exposureSimulationService
    ) {
        this.xvaReferenceDataService = xvaReferenceDataService;
        this.exposureSimulationService = exposureSimulationService;
        this.calculator = new SimplifiedCvaCalculator();
    }

    @Transactional(readOnly = true)
    public CvaNettingSetCalculationResult calculate(CvaNettingSetCalculationCommand command) {
        NettingSet nettingSet = xvaReferenceDataService.getOperableNettingSet(command.nettingSetId());
        if (nettingSet.portfolios().isEmpty()) {
            throw new IllegalArgumentException("Netting set must contain at least one active portfolio");
        }

        List<ExposureSimulationResult> exposureResults = nettingSet.portfolios()
                .stream()
                .map(portfolio -> exposureSimulationService.simulate(portfolioExposureCommand(command, portfolio)))
                .toList();
        validateExposureCurrencies(nettingSet, exposureResults);
        List<ExposurePoint> grossPoints = grossExposurePoints(exposureResults);
        List<ExposurePoint> nettedPoints = applyCollateral(grossPoints, nettingSet.collateralAmount().doubleValue());
        CvaResult uncollateralizedCvaResult = calculator.calculate(new CvaInput(
                command.valuationDate(),
                grossPoints,
                command.lossGivenDefault(),
                command.counterpartyHazardRate(),
                command.discountRate(),
                command.creditCurve(),
                command.discountCurve()
        ));
        CvaResult cvaResult = calculator.calculate(new CvaInput(
                command.valuationDate(),
                nettedPoints,
                command.lossGivenDefault(),
                command.counterpartyHazardRate(),
                command.discountRate(),
                command.creditCurve(),
                command.discountCurve()
        ));
        double collateralBenefit = Math.max(uncollateralizedCvaResult.cva() - cvaResult.cva(), 0.0);
        double collateralBenefitPercent = uncollateralizedCvaResult.cva() > 0.0
                ? collateralBenefit / uncollateralizedCvaResult.cva()
                : 0.0;
        Map<LocalDate, Double> grossExposureByDate = grossPoints.stream()
                .collect(java.util.stream.Collectors.toMap(ExposurePoint::date, ExposurePoint::expectedExposure));
        List<com.nexusxva.cva.domain.CvaPoint> collateralizedPoints = cvaResult.points().stream()
                .map(point -> {
                    double grossExposure = grossExposureByDate.getOrDefault(point.date(), point.expectedExposure());
                    return new com.nexusxva.cva.domain.CvaPoint(
                            point.date(),
                            grossExposure,
                            Math.max(grossExposure - point.expectedExposure(), 0.0),
                            point.expectedExposure(),
                            point.discountFactor(),
                            point.survivalProbability(),
                            point.defaultProbabilityIncrement(),
                            point.discountedExpectedExposure(),
                            point.cvaContribution()
                    );
                })
                .toList();

        return new CvaNettingSetCalculationResult(
                nettingSet.id(),
                nettingSet.counterpartyId(),
                nettingSet.counterpartyName(),
                nettingSet.name(),
                nettingSet.baseCurrency(),
                nettingSet.collateralAmount(),
                nettingSet.collateralCurrency(),
                nettingSet.portfolios().size(),
                command.valuationDate(),
                MODEL,
                exposureResults.getFirst().model(),
                command.paths(),
                command.timeSteps(),
                command.pfeConfidenceLevel(),
                command.lossGivenDefault(),
                command.counterpartyHazardRate(),
                command.discountRate(),
                cvaResult.creditMethod(),
                cvaResult.discountMethod(),
                uncollateralizedCvaResult.cva(),
                collateralBenefit,
                collateralBenefitPercent,
                cvaResult.cva(),
                collateralizedPoints
        );
    }

    private void validateExposureCurrencies(NettingSet nettingSet, List<ExposureSimulationResult> exposureResults) {
        for (ExposureSimulationResult exposureResult : exposureResults) {
            if (!nettingSet.baseCurrency().equals(exposureResult.baseCurrency())) {
                throw new IllegalArgumentException("Exposure currency must match netting set baseCurrency");
            }
        }
    }

    private ExposureSimulationCommand portfolioExposureCommand(
            CvaNettingSetCalculationCommand command,
            NettingSetPortfolio portfolio
    ) {
        return new ExposureSimulationCommand(
                portfolio.portfolioId(),
                command.valuationDate(),
                command.horizonDays(),
                command.timeSteps(),
                command.paths(),
                command.seed(),
                command.pfeConfidenceLevel()
        );
    }

    private List<ExposurePoint> grossExposurePoints(List<ExposureSimulationResult> exposureResults) {
        Map<LocalDate, double[]> totalsByDate = new LinkedHashMap<>();
        for (ExposureSimulationResult result : exposureResults) {
            for (ExposurePoint point : result.points()) {
                double[] totals = totalsByDate.computeIfAbsent(point.date(), ignored -> new double[3]);
                totals[0] += point.expectedExposure();
                totals[1] += point.expectedNegativeExposure();
                totals[2] += point.pfe();
            }
        }
        return totalsByDate.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .map(entry -> {
                    double[] totals = entry.getValue();
                    return new ExposurePoint(
                            entry.getKey(),
                            totals[0],
                            totals[1],
                            totals[2]
                    );
                })
                .toList();
    }

    private List<ExposurePoint> applyCollateral(List<ExposurePoint> grossPoints, double collateralAmount) {
        return grossPoints.stream()
                .map(point -> new ExposurePoint(
                        point.date(),
                        Math.max(point.expectedExposure() - collateralAmount, 0.0),
                        point.expectedNegativeExposure(),
                        Math.max(point.pfe() - collateralAmount, 0.0)
                ))
                .toList();
    }
}
