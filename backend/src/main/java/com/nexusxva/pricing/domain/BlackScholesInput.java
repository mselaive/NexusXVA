package com.nexusxva.pricing.domain;

import com.nexusxva.instruments.domain.OptionType;

public record BlackScholesInput(
        OptionType optionType,
        double spot,
        double strike,
        double timeToMaturityYears,
        double riskFreeRate,
        double volatility,
        double dividendYield
) {

    public BlackScholesInput(
            OptionType optionType,
            double spot,
            double strike,
            double timeToMaturityYears,
            double riskFreeRate,
            double volatility
    ) {
        this(optionType, spot, strike, timeToMaturityYears, riskFreeRate, volatility, 0.0);
    }

    public BlackScholesInput {
        if (optionType == null) {
            throw new IllegalArgumentException("optionType is required");
        }
        requirePositiveFinite("spot", spot);
        requirePositiveFinite("strike", strike);
        requirePositiveFinite("timeToMaturityYears", timeToMaturityYears);
        requireFinite("riskFreeRate", riskFreeRate);
        requirePositiveFinite("volatility", volatility);
        requireNonNegativeFinite("dividendYield", dividendYield);
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
