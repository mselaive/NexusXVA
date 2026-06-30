package com.nexusxva.portfolio.api;

import com.nexusxva.portfolio.domain.CashEquityPosition;
import com.nexusxva.portfolio.domain.PositionLifecycleStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CashEquityPositionResponse(
        UUID id,
        UUID portfolioId,
        String underlyingSymbol,
        BigDecimal quantity,
        BigDecimal executionPrice,
        PositionLifecycleStatus lifecycleStatus,
        Instant createdAt,
        Instant updatedAt
) {

    static CashEquityPositionResponse from(CashEquityPosition position) {
        return new CashEquityPositionResponse(
                position.id(),
                position.portfolioId(),
                position.underlyingSymbol(),
                position.quantity(),
                position.executionPrice(),
                position.lifecycleStatus(),
                position.createdAt(),
                position.updatedAt()
        );
    }
}
