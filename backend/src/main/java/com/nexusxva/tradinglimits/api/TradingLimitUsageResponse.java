package com.nexusxva.tradinglimits.api;

import com.nexusxva.tradinglimits.domain.TradingLimitUsage;
import java.math.BigDecimal;
import java.time.Instant;

public record TradingLimitUsageResponse(
        long tradesThisHour,
        long tradesToday,
        BigDecimal notionalThisHour,
        BigDecimal notionalToday,
        Instant hourEndsAt,
        Instant dayEndsAt
) {

    static TradingLimitUsageResponse from(TradingLimitUsage usage) {
        return new TradingLimitUsageResponse(
                usage.tradesThisHour(),
                usage.tradesToday(),
                usage.notionalThisHour(),
                usage.notionalToday(),
                usage.hourEndsAt(),
                usage.dayEndsAt()
        );
    }
}

