package com.nexusxva.cva.domain;

import com.nexusxva.exposure.domain.ExposurePoint;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SimplifiedCvaCalculator {

    public CvaResult calculate(CvaInput input) {
        List<ExposurePoint> exposurePoints = input.exposurePoints()
                .stream()
                .sorted(Comparator.comparing(ExposurePoint::date))
                .toList();

        List<CvaPoint> cvaPoints = new ArrayList<>();
        double cva = 0.0;
        double previousSurvivalProbability = 1.0;

        for (ExposurePoint exposurePoint : exposurePoints) {
            double timeYears = ChronoUnit.DAYS.between(input.valuationDate(), exposurePoint.date()) / 365.0;
            double discountFactor = Math.exp(-input.discountRate() * timeYears);
            double survivalProbability = Math.exp(-input.counterpartyHazardRate() * timeYears);
            double defaultProbabilityIncrement = previousSurvivalProbability - survivalProbability;
            double discountedExpectedExposure = discountFactor * exposurePoint.expectedExposure();
            double cvaContribution = input.lossGivenDefault() * discountedExpectedExposure * defaultProbabilityIncrement;

            CvaPoint point = new CvaPoint(
                    exposurePoint.date(),
                    exposurePoint.expectedExposure(),
                    discountFactor,
                    survivalProbability,
                    defaultProbabilityIncrement,
                    discountedExpectedExposure,
                    cvaContribution
            );
            cvaPoints.add(point);
            cva += cvaContribution;
            previousSurvivalProbability = survivalProbability;
        }

        return new CvaResult(cva, cvaPoints);
    }
}
