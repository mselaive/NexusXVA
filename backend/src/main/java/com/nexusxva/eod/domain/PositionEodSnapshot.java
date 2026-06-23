package com.nexusxva.eod.domain;

import java.time.Instant;
import java.util.UUID;

public record PositionEodSnapshot(
        UUID positionId,
        String underlyingSymbol,
        double quantity,
        double unitPrice,
        double marketValue,
        Double executionPrice,
        Double tradeValue,
        Double unrealizedPnl,
        Instant marketDataAsOf,
        String marketDataSource,
        boolean stale
) {
}
