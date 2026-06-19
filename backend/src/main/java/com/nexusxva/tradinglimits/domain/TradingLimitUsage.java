package com.nexusxva.tradinglimits.domain;

import java.math.BigDecimal;
import java.time.Instant;

public record TradingLimitUsage(
        long tradesThisHour,
        long tradesToday,
        BigDecimal notionalThisHour,
        BigDecimal notionalToday,
        Instant hourEndsAt,
        Instant dayEndsAt
) {
}

