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
        EodRunStatus status,
        Instant voidedAt,
        UUID voidedByUserId,
        String voidReason,
        UUID correctionOfRunId,
        List<PositionEodSnapshot> positions
) {
    public PortfolioEodSnapshot(
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
        this(
                id,
                portfolioId,
                businessDate,
                baseCurrency,
                totalMarketValue,
                totalTradeValue,
                totalUnrealizedPnl,
                positionsWithoutExecutionPrice,
                capturedAt,
                source,
                EodRunStatus.ACTIVE,
                null,
                null,
                null,
                null,
                positions
        );
    }

    public PortfolioEodSnapshot {
        if (status == null) {
            status = EodRunStatus.ACTIVE;
        }
        positions = List.copyOf(positions);
    }
}
