package com.nexusxva.tradinglimits.application;

import com.nexusxva.tradinglimits.domain.TradingLimitPolicy;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record UpdateTradingLimitCommand(
        Integer maxTradesPerHour,
        Integer maxTradesPerDay,
        BigDecimal maxNotionalPerHour,
        BigDecimal maxNotionalPerDay,
        boolean active,
        Long version
) {

    public UpdateTradingLimitCommand {
        new TradingLimitPolicy(
                UUID.randomUUID(),
                maxTradesPerHour,
                maxTradesPerDay,
                maxNotionalPerHour,
                maxNotionalPerDay,
                "USD",
                active,
                Instant.EPOCH,
                Instant.EPOCH,
                null,
                null,
                null,
                0
        );
    }
}
