package com.nexusxva.cva.domain;

import java.time.LocalDate;

public record CreditCurvePoint(
        LocalDate date,
        Double survivalProbability,
        Double cumulativeDefaultProbability
) {

    public CreditCurvePoint {
        if (date == null) {
            throw new IllegalArgumentException("creditCurve date is required");
        }
        boolean hasSurvival = survivalProbability != null;
        boolean hasCumulativeDefault = cumulativeDefaultProbability != null;
        if (hasSurvival == hasCumulativeDefault) {
            throw new IllegalArgumentException(
                    "creditCurve point must contain exactly one of survivalProbability or cumulativeDefaultProbability"
            );
        }
        if (hasSurvival) {
            requireProbability("survivalProbability", survivalProbability);
        }
        if (hasCumulativeDefault) {
            requireProbability("cumulativeDefaultProbability", cumulativeDefaultProbability);
        }
    }

    double resolvedSurvivalProbability() {
        if (survivalProbability != null) {
            return survivalProbability;
        }
        return 1.0 - cumulativeDefaultProbability;
    }

    boolean usesCumulativeDefaultProbability() {
        return cumulativeDefaultProbability != null;
    }

    private static void requireProbability(String field, double value) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(field + " must be between 0 and 1");
        }
    }
}
