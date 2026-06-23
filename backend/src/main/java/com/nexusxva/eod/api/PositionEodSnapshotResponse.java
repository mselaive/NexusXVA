package com.nexusxva.eod.api;

import com.nexusxva.eod.domain.PositionEodSnapshot;
import java.time.Instant;
import java.util.UUID;

public record PositionEodSnapshotResponse(
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
    static PositionEodSnapshotResponse from(PositionEodSnapshot snapshot) {
        return new PositionEodSnapshotResponse(
                snapshot.positionId(),
                snapshot.underlyingSymbol(),
                snapshot.quantity(),
                snapshot.unitPrice(),
                snapshot.marketValue(),
                snapshot.executionPrice(),
                snapshot.tradeValue(),
                snapshot.unrealizedPnl(),
                snapshot.marketDataAsOf(),
                snapshot.marketDataSource(),
                snapshot.stale()
        );
    }
}
