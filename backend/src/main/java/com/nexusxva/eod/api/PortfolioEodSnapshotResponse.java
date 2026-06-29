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
        String status,
        Instant voidedAt,
        UUID voidedByUserId,
        String voidReason,
        UUID correctionOfRunId,
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
                snapshot.status().name(),
                snapshot.voidedAt(),
                snapshot.voidedByUserId(),
                snapshot.voidReason(),
                snapshot.correctionOfRunId(),
                snapshot.positions().stream().map(PositionEodSnapshotResponse::from).toList()
        );
    }
}
