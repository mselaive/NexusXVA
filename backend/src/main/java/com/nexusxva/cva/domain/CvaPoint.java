package com.nexusxva.cva.domain;

import java.time.LocalDate;

public record CvaPoint(
        LocalDate date,
        double expectedExposure,
        double discountFactor,
        double survivalProbability,
        double defaultProbabilityIncrement,
        double discountedExpectedExposure,
        double cvaContribution
) {

    public CvaPoint {
        if (date == null) {
            throw new IllegalArgumentException("cva point date is required");
        }
        requireNonNegativeFinite("expectedExposure", expectedExposure);
        requireNonNegativeFinite("discountFactor", discountFactor);
        requireNonNegativeFinite("survivalProbability", survivalProbability);
        requireNonNegativeFinite("defaultProbabilityIncrement", defaultProbabilityIncrement);
        requireNonNegativeFinite("discountedExpectedExposure", discountedExpectedExposure);
        requireNonNegativeFinite("cvaContribution", cvaContribution);
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
