package com.nexusxva.cva.domain;

import java.time.LocalDate;

public record CvaPoint(
        LocalDate date,
        double grossExpectedExposure,
        double collateralApplied,
        double expectedExposure,
        double discountFactor,
        double survivalProbability,
        double defaultProbabilityIncrement,
        double discountedExpectedExposure,
        double cvaContribution
) {

    public CvaPoint(
            LocalDate date,
            double expectedExposure,
            double discountFactor,
            double survivalProbability,
            double defaultProbabilityIncrement,
            double discountedExpectedExposure,
            double cvaContribution
    ) {
        this(
                date,
                expectedExposure,
                0.0,
                expectedExposure,
                discountFactor,
                survivalProbability,
                defaultProbabilityIncrement,
                discountedExpectedExposure,
                cvaContribution
        );
    }

    public CvaPoint {
        if (date == null) {
            throw new IllegalArgumentException("cva point date is required");
        }
        requireNonNegativeFinite("grossExpectedExposure", grossExpectedExposure);
        requireNonNegativeFinite("collateralApplied", collateralApplied);
        requireNonNegativeFinite("expectedExposure", expectedExposure);
        requireNonNegativeFinite("discountFactor", discountFactor);
        requireNonNegativeFinite("survivalProbability", survivalProbability);
        requireNonNegativeFinite("defaultProbabilityIncrement", defaultProbabilityIncrement);
        requireNonNegativeFinite("discountedExpectedExposure", discountedExpectedExposure);
        requireNonNegativeFinite("cvaContribution", cvaContribution);
        if (Math.abs(grossExpectedExposure - collateralApplied - expectedExposure) > 1.0e-7) {
            throw new IllegalArgumentException("grossExpectedExposure minus collateralApplied must equal expectedExposure");
        }
    }

    private static void requireNonNegativeFinite(String field, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(field + " must be finite");
        }
        if (value < 0.0) {
            throw new IllegalArgumentException(field + " must be greater than or equal to zero");
        }
    }
}
