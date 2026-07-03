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
        double optionMarketValue,
        double cashEquityMarketValue,
        double optionUnrealizedPnl,
        double cashEquityUnrealizedPnl,
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
        double optionMarketValue = snapshot.positions().stream()
                .filter(position -> "EUROPEAN_OPTION".equals(position.instrumentType()))
                .mapToDouble(position -> position.marketValue())
                .sum();
        double cashEquityMarketValue = snapshot.positions().stream()
                .filter(position -> "CASH_EQUITY".equals(position.instrumentType()))
                .mapToDouble(position -> position.marketValue())
                .sum();
        double optionUnrealizedPnl = snapshot.positions().stream()
                .filter(position -> "EUROPEAN_OPTION".equals(position.instrumentType()))
                .mapToDouble(position -> position.unrealizedPnl() == null ? 0.0 : position.unrealizedPnl())
                .sum();
        double cashEquityUnrealizedPnl = snapshot.positions().stream()
                .filter(position -> "CASH_EQUITY".equals(position.instrumentType()))
                .mapToDouble(position -> position.unrealizedPnl() == null ? 0.0 : position.unrealizedPnl())
                .sum();
        return new PortfolioEodSnapshotResponse(
                snapshot.id(),
                snapshot.portfolioId(),
                snapshot.businessDate(),
                snapshot.baseCurrency(),
                snapshot.totalMarketValue(),
                snapshot.totalTradeValue(),
                snapshot.totalUnrealizedPnl(),
                optionMarketValue,
                cashEquityMarketValue,
                optionUnrealizedPnl,
                cashEquityUnrealizedPnl,
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
