package com.nexusxva.eod.api;

import com.nexusxva.eod.domain.PortfolioEodSnapshot;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PortfolioEodSnapshotResponse(
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
        List<PositionEodSnapshotResponse> positions
) {
    static PortfolioEodSnapshotResponse from(PortfolioEodSnapshot snapshot) {
        return new PortfolioEodSnapshotResponse(
                snapshot.id(),
                snapshot.portfolioId(),
                snapshot.businessDate(),
                snapshot.baseCurrency(),
                snapshot.totalMarketValue(),
                snapshot.totalTradeValue(),
                snapshot.totalUnrealizedPnl(),
                snapshot.positionsWithoutExecutionPrice(),
                snapshot.capturedAt(),
                snapshot.source(),
                snapshot.positions().stream().map(PositionEodSnapshotResponse::from).toList()
        );
    }
}
