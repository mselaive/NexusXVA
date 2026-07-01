package com.nexusxva.xva.application;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

public record CreateNettingSetCommand(
        UUID counterpartyId,
        String name,
        String baseCurrency,
        BigDecimal collateralAmount,
        String collateralCurrency
) {
    public CreateNettingSetCommand {
        if (counterpartyId == null) {
            throw new IllegalArgumentException("counterpartyId is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("netting set name is required");
        }
        name = name.trim();
        baseCurrency = normalizeCurrency(baseCurrency == null ? "USD" : baseCurrency, "baseCurrency");
        collateralCurrency = normalizeCurrency(collateralCurrency == null ? baseCurrency : collateralCurrency, "collateralCurrency");
        collateralAmount = collateralAmount == null ? BigDecimal.ZERO : collateralAmount;
        if (collateralAmount.signum() < 0) {
            throw new IllegalArgumentException("collateralAmount must be greater than or equal to zero");
        }
        if (!baseCurrency.equals(collateralCurrency)) {
            throw new IllegalArgumentException("collateralCurrency must match baseCurrency in V1");
        }
    }

    private static String normalizeCurrency(String value, String field) {
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException(field + " must be a 3-letter currency code");
        }
        return normalized;
    }
}
