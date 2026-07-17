package com.nexusxva.portfolio.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CashEquityLot(
        UUID id,
        UUID positionId,
        CashEquityLotType lotType,
        BigDecimal quantity,
        BigDecimal executionPrice,
        BigDecimal averageCost,
        BigDecimal realizedPnl,
        Instant executedAt,
        Instant createdAt
) {
    public CashEquityLot {
        if (id == null) {
            throw new IllegalArgumentException("cash equity lot id is required");
        }
        if (positionId == null) {
            throw new IllegalArgumentException("cash equity lot positionId is required");
        }
        if (lotType == null) {
            throw new IllegalArgumentException("cash equity lotType is required");
        }
        if (quantity == null || quantity.signum() == 0) {
            throw new IllegalArgumentException("cash equity lot quantity must be non-zero");
        }
        if (executionPrice != null && executionPrice.signum() < 0) {
            throw new IllegalArgumentException("cash equity lot executionPrice must be greater than or equal to zero");
        }
        if (averageCost != null && averageCost.signum() < 0) {
            throw new IllegalArgumentException("cash equity lot averageCost must be greater than or equal to zero");
        }
        if (realizedPnl == null) {
            realizedPnl = BigDecimal.ZERO;
        }
        if (executedAt == null) {
            throw new IllegalArgumentException("cash equity lot executedAt is required");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("cash equity lot createdAt is required");
        }
    }
}
