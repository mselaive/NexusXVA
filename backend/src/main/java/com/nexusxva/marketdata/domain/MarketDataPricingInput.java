package com.nexusxva.marketdata.domain;

import java.time.Instant;
import java.util.Locale;

public record MarketDataPricingInput(
        String symbol,
        double spot,
        double volatility,
        double riskFreeRate,
        double dividendYield,
        String currency,
        Instant asOf,
        String source,
        boolean stale
) {

    public MarketDataPricingInput(
            String symbol,
            double spot,
            double volatility,
            double riskFreeRate,
            String currency,
            Instant asOf,
            String source,
            boolean stale
    ) {
        this(symbol, spot, volatility, riskFreeRate, 0.0, currency, asOf, source, stale);
    }

    public MarketDataPricingInput {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("pricing input symbol is required");
        }
        symbol = symbol.trim().toUpperCase(Locale.ROOT);
        requirePositiveFinite("spot", spot);
        requirePositiveFinite("volatility", volatility);
        requireFinite("riskFreeRate", riskFreeRate);
        requireNonNegativeFinite("dividendYield", dividendYield);
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("pricing input currency is required");
        }
        currency = currency.trim().toUpperCase(Locale.ROOT);
        if (!currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("pricing input currency must be a 3-letter currency code");
        }
        if (asOf == null) {
            throw new IllegalArgumentException("pricing input asOf is required");
        }
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("pricing input source is required");
        }
        source = source.trim().toUpperCase(Locale.ROOT);
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
