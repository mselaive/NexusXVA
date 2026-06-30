package com.nexusxva.portfolio.application;

import com.nexusxva.portfolio.domain.CashEquityPosition;
import com.nexusxva.portfolio.domain.PositionLifecycleStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public record AddCashEquityPositionCommand(
        String underlyingSymbol,
        BigDecimal quantity,
        BigDecimal executionPrice
) {

    public AddCashEquityPositionCommand {
        CashEquityPosition validated = new CashEquityPosition(
                UUID.randomUUID(),
                UUID.randomUUID(),
                underlyingSymbol,
                quantity,
                executionPrice,
                PositionLifecycleStatus.ACTIVE,
                Instant.EPOCH,
                Instant.EPOCH
        );
        underlyingSymbol = validated.underlyingSymbol().trim().toUpperCase(Locale.ROOT);
    }
}
