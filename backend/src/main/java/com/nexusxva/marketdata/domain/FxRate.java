package com.nexusxva.marketdata.domain;

import java.time.Instant;
import java.util.Locale;

public record FxRate(
        String sourceCurrency,
        String targetCurrency,
        double rate,
        Instant asOf,
        String source,
        boolean stale
) {

    public FxRate {
        sourceCurrency = normalizeCurrency(sourceCurrency, "sourceCurrency");
        targetCurrency = normalizeCurrency(targetCurrency, "targetCurrency");
        if (!Double.isFinite(rate) || rate <= 0.0) {
            throw new IllegalArgumentException("FX rate must be finite and greater than zero");
        }
        if (asOf == null) {
            throw new IllegalArgumentException("FX rate asOf is required");
        }
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("FX rate source is required");
        }
        source = source.trim();
    }

    private static String normalizeCurrency(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException(field + " must be a 3-letter currency code");
        }
        return normalized;
    }
}
