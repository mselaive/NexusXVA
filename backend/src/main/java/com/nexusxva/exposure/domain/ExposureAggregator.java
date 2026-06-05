package com.nexusxva.exposure.domain;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public class ExposureAggregator {

    public List<ExposurePoint> aggregate(
            List<LocalDate> dates,
            double[][] portfolioValues,
            double pfeConfidenceLevel
    ) {
        if (dates == null || dates.isEmpty()) {
            throw new IllegalArgumentException("exposure dates are required");
        }
        if (portfolioValues == null || portfolioValues.length == 0) {
            throw new IllegalArgumentException("portfolio values are required");
        }
        if (!Double.isFinite(pfeConfidenceLevel) || pfeConfidenceLevel <= 0.0 || pfeConfidenceLevel >= 1.0) {
            throw new IllegalArgumentException("pfeConfidenceLevel must be between 0 and 1");
        }

        int paths = portfolioValues.length;
        int timeSteps = dates.size();
        for (double[] pathValues : portfolioValues) {
            if (pathValues == null || pathValues.length != timeSteps) {
                throw new IllegalArgumentException("portfolio values must match exposure dates");
            }
        }

        return java.util.stream.IntStream.range(0, timeSteps)
                .mapToObj(step -> aggregateStep(dates.get(step), portfolioValues, step, pfeConfidenceLevel))
                .toList();
    }

    private ExposurePoint aggregateStep(
            LocalDate date,
            double[][] portfolioValues,
            int step,
            double pfeConfidenceLevel
    ) {
        double expectedExposure = 0.0;
        double expectedNegativeExposure = 0.0;
        double[] positiveExposures = new double[portfolioValues.length];

        for (int path = 0; path < portfolioValues.length; path++) {
            double value = portfolioValues[path][step];
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("portfolio value must be finite");
            }

            double positiveExposure = Math.max(value, 0.0);
            double negativeExposure = Math.max(-value, 0.0);
            positiveExposures[path] = positiveExposure;
            expectedExposure += positiveExposure;
            expectedNegativeExposure += negativeExposure;
        }

        expectedExposure /= portfolioValues.length;
        expectedNegativeExposure /= portfolioValues.length;
        Arrays.sort(positiveExposures);
        int percentileIndex = Math.max(0, (int) Math.ceil(pfeConfidenceLevel * positiveExposures.length) - 1);

        return new ExposurePoint(
                date,
                expectedExposure,
                expectedNegativeExposure,
                positiveExposures[percentileIndex]
        );
    }
}
