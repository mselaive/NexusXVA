package com.nexusxva.portfolio.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public record CashEquityPosition(
        UUID id,
        UUID portfolioId,
        String underlyingSymbol,
        BigDecimal quantity,
        BigDecimal executionPrice,
        PositionLifecycleStatus lifecycleStatus,
        Instant createdAt,
        Instant updatedAt
) {

    public CashEquityPosition {
        if (id == null) {
            throw new IllegalArgumentException("cash equity position id is required");
        }
        if (portfolioId == null) {
            throw new IllegalArgumentException("portfolio id is required");
        }
        if (underlyingSymbol == null || underlyingSymbol.isBlank()) {
            throw new IllegalArgumentException("underlyingSymbol is required");
        }
        underlyingSymbol = underlyingSymbol.trim().toUpperCase(Locale.ROOT);
        if (!underlyingSymbol.matches("[A-Z0-9._-]{1,32}")) {
            throw new IllegalArgumentException("underlyingSymbol must use 1-32 letters, numbers, dots, underscores, or hyphens");
        }
        if (quantity == null || quantity.signum() == 0) {
            throw new IllegalArgumentException("quantity must be non-zero");
        }
        if (executionPrice != null && executionPrice.signum() < 0) {
            throw new IllegalArgumentException("executionPrice must be greater than or equal to zero");
        }
        if (lifecycleStatus == null) {
            throw new IllegalArgumentException("cash equity lifecycleStatus is required");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("cash equity createdAt is required");
        }
        if (updatedAt == null) {
            throw new IllegalArgumentException("cash equity updatedAt is required");
        }
    }
}
