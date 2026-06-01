package com.nexusxva.portfolio.application;

import java.util.Locale;

public record UpdatePortfolioCommand(
        String name,
        String description,
        String baseCurrency
) {

    public UpdatePortfolioCommand {
        if (name != null) {
            if (name.isBlank()) {
                throw new IllegalArgumentException("portfolio name is required");
            }
            name = name.trim();
        }

        if (description != null) {
            if (description.length() > 500) {
                throw new IllegalArgumentException("portfolio description must be at most 500 characters");
            }
            description = description.isBlank() ? "" : description.trim();
        }

        if (baseCurrency != null) {
            if (baseCurrency.isBlank()) {
                throw new IllegalArgumentException("baseCurrency must be a 3-letter currency code");
            }
            baseCurrency = baseCurrency.trim().toUpperCase(Locale.ROOT);
            if (!baseCurrency.matches("[A-Z]{3}")) {
                throw new IllegalArgumentException("baseCurrency must be a 3-letter currency code");
            }
        }
    }
}
