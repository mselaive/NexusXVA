package com.nexusxva.tradinglimits.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TradingLimitPolicy(
        UUID userId,
        Integer maxTradesPerHour,
        Integer maxTradesPerDay,
        BigDecimal maxNotionalPerHour,
        BigDecimal maxNotionalPerDay,
        String notionalCurrency,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        UUID updatedByUserId,
        String updatedByUsername,
        String updatedByDisplayName,
        long version
) {

    public TradingLimitPolicy {
        validatePositive(maxTradesPerHour, "maxTradesPerHour");
        validatePositive(maxTradesPerDay, "maxTradesPerDay");
        validatePositive(maxNotionalPerHour, "maxNotionalPerHour");
        validatePositive(maxNotionalPerDay, "maxNotionalPerDay");
        if (maxTradesPerHour != null && maxTradesPerDay != null && maxTradesPerDay < maxTradesPerHour) {
            throw new IllegalArgumentException("maxTradesPerDay must be greater than or equal to maxTradesPerHour");
        }
        if (maxNotionalPerHour != null
                && maxNotionalPerDay != null
                && maxNotionalPerDay.compareTo(maxNotionalPerHour) < 0) {
            throw new IllegalArgumentException("maxNotionalPerDay must be greater than or equal to maxNotionalPerHour");
        }
        if (!"USD".equals(notionalCurrency)) {
            throw new IllegalArgumentException("notionalCurrency must be USD");
        }
    }

    public boolean hasNotionalLimit() {
        return maxNotionalPerHour != null || maxNotionalPerDay != null;
    }

    private static void validatePositive(Integer value, String field) {
        if (value != null && value <= 0) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
    }

    private static void validatePositive(BigDecimal value, String field) {
        if (value != null && value.signum() <= 0) {
            throw new IllegalArgumentException(field + " must be greater than zero");
        }
    }
}

