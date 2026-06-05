package com.nexusxva.simulation.domain;

public record GbmParameters(
        double initialSpot,
        double riskFreeRate,
        double dividendYield,
        double volatility
) {

    public GbmParameters {
        requirePositiveFinite("initialSpot", initialSpot);
        requireFinite("riskFreeRate", riskFreeRate);
        requireNonNegativeFinite("dividendYield", dividendYield);
        requireNonNegativeFinite("volatility", volatility);
    }

    private static void requirePositiveFinite(String field, double value) {
        requireFinite(field, value);
        if (value <= 0.0) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
    }

    private static void requireFinite(String field, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(field + " must be finite");
        }
    }

    private static void requireNonNegativeFinite(String field, double value) {
        requireFinite(field, value);
        if (value < 0.0) {
            throw new IllegalArgumentException(field + " must be greater than or equal to zero");
        }
    }
}
