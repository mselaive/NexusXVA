package com.nexusxva.portfolio.api;

import com.nexusxva.portfolio.domain.CashEquityLot;
import com.nexusxva.portfolio.domain.CashEquityLotType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CashEquityLotResponse(
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
    static CashEquityLotResponse from(CashEquityLot lot) {
        return new CashEquityLotResponse(
                lot.id(),
                lot.positionId(),
                lot.lotType(),
                lot.quantity(),
                lot.executionPrice(),
                lot.averageCost(),
                lot.realizedPnl(),
                lot.executedAt(),
                lot.createdAt()
        );
    }
}
