package com.nexusxva.portfolio.api;

import com.nexusxva.portfolio.domain.CashEquityPosition;
import com.nexusxva.portfolio.domain.PositionLifecycleStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CashEquityPositionResponse(
        UUID id,
        UUID portfolioId,
        String underlyingSymbol,
        BigDecimal quantity,
        BigDecimal executionPrice,
        BigDecimal averageCost,
        BigDecimal realizedPnl,
        PositionLifecycleStatus lifecycleStatus,
        Instant createdAt,
        Instant updatedAt,
        List<CashEquityLotResponse> lots
) {

    static CashEquityPositionResponse from(CashEquityPosition position) {
        return new CashEquityPositionResponse(
                position.id(),
                position.portfolioId(),
                position.underlyingSymbol(),
                position.quantity(),
                position.executionPrice(),
                position.averageCost(),
                position.realizedPnl(),
                position.lifecycleStatus(),
                position.createdAt(),
                position.updatedAt(),
                position.lots().stream().map(CashEquityLotResponse::from).toList()
        );
    }
}
