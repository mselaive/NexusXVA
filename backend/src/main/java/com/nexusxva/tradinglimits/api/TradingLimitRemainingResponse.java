package com.nexusxva.tradinglimits.api;

import com.nexusxva.tradinglimits.domain.TradingLimitPolicy;
import com.nexusxva.tradinglimits.domain.TradingLimitUsage;
import java.math.BigDecimal;

public record TradingLimitRemainingResponse(
        Long tradesThisHour,
        Long tradesToday,
        BigDecimal notionalThisHour,
        BigDecimal notionalToday
) {

    static TradingLimitRemainingResponse from(TradingLimitPolicy policy, TradingLimitUsage usage) {
        if (policy == null || !policy.active()) {
            return new TradingLimitRemainingResponse(null, null, null, null);
        }
        return new TradingLimitRemainingResponse(
                remaining(policy.maxTradesPerHour(), usage.tradesThisHour()),
                remaining(policy.maxTradesPerDay(), usage.tradesToday()),
                remaining(policy.maxNotionalPerHour(), usage.notionalThisHour()),
                remaining(policy.maxNotionalPerDay(), usage.notionalToday())
        );
    }

    private static Long remaining(Integer maximum, long usage) {
        return maximum == null ? null : Math.max(maximum.longValue() - usage, 0);
    }

    private static BigDecimal remaining(BigDecimal maximum, BigDecimal usage) {
        if (maximum == null) {
            return null;
        }
        return maximum.subtract(usage).max(BigDecimal.ZERO);
    }
}

