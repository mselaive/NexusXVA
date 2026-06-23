package com.nexusxva.eod.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PortfolioEodSnapshot(
        UUID id,
        UUID portfolioId,
        LocalDate businessDate,
        String baseCurrency,
        double totalMarketValue,
        double totalTradeValue,
        double totalUnrealizedPnl,
        int positionsWithoutExecutionPrice,
        Instant capturedAt,
        String source,
        List<PositionEodSnapshot> positions
) {
    public PortfolioEodSnapshot {
        positions = List.copyOf(positions);
    }
}
