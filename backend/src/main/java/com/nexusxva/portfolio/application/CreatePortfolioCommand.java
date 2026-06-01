package com.nexusxva.portfolio.application;

import java.util.Locale;

public record CreatePortfolioCommand(
        String name,
        String description,
        String baseCurrency
) {

    public CreatePortfolioCommand {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("portfolio name is required");
        }
        if (description != null && description.length() > 500) {
            throw new IllegalArgumentException("portfolio description must be at most 500 characters");
        }

        name = name.trim();
        description = normalizeDescription(description);
        baseCurrency = normalizeBaseCurrency(baseCurrency);
    }

    private static String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }

    private static String normalizeBaseCurrency(String baseCurrency) {
        String normalized = baseCurrency;
        if (normalized == null || normalized.isBlank()) {
            normalized = "USD";
        }
        normalized = normalized.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("baseCurrency must be a 3-letter currency code");
        }
        return normalized;
    }
}
