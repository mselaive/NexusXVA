package com.nexusxva.tradinglimits.domain;

import java.util.UUID;

public record TradingLimitSnapshot(
        UUID userId,
        String username,
        String displayName,
        String status,
        TradingLimitPolicy policy,
        TradingLimitUsage usage
) {
}

