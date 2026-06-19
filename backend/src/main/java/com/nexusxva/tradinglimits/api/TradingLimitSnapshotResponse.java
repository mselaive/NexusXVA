package com.nexusxva.tradinglimits.api;

import com.nexusxva.tradinglimits.domain.TradingLimitSnapshot;
import java.util.UUID;

public record TradingLimitSnapshotResponse(
        UUID userId,
        String username,
        String displayName,
        String status,
        TradingLimitPolicyResponse policy,
        TradingLimitUsageResponse usage,
        TradingLimitRemainingResponse remaining
) {

    static TradingLimitSnapshotResponse from(TradingLimitSnapshot snapshot) {
        return new TradingLimitSnapshotResponse(
                snapshot.userId(),
                snapshot.username(),
                snapshot.displayName(),
                snapshot.status(),
                TradingLimitPolicyResponse.from(snapshot.policy()),
                TradingLimitUsageResponse.from(snapshot.usage()),
                TradingLimitRemainingResponse.from(snapshot.policy(), snapshot.usage())
        );
    }
}

