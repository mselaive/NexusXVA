package com.nexusxva.tradinglimits.api;

import com.nexusxva.tradinglimits.domain.TradingLimitPolicy;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TradingLimitPolicyResponse(
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

    static TradingLimitPolicyResponse from(TradingLimitPolicy policy) {
        if (policy == null) {
            return null;
        }
        return new TradingLimitPolicyResponse(
                policy.maxTradesPerHour(),
                policy.maxTradesPerDay(),
                policy.maxNotionalPerHour(),
                policy.maxNotionalPerDay(),
                policy.notionalCurrency(),
                policy.active(),
                policy.createdAt(),
                policy.updatedAt(),
                policy.updatedByUserId(),
                policy.updatedByUsername(),
                policy.updatedByDisplayName(),
                policy.version()
        );
    }
}

