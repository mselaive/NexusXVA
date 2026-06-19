package com.nexusxva.tradinglimits.application;

import com.nexusxva.tradebooking.domain.BookingActor;
import com.nexusxva.tradinglimits.domain.TradingLimitPolicy;
import com.nexusxva.tradinglimits.domain.TradingLimitSnapshot;
import com.nexusxva.tradinglimits.domain.TradingLimitUsage;
import com.nexusxva.tradinglimits.domain.TradingLimitWindows;
import java.util.Optional;
import java.util.UUID;

public interface TradingLimitStore {

    Optional<TradingLimitPolicy> findPolicy(UUID userId);

    Optional<TradingLimitPolicy> findPolicyForUpdate(UUID userId);

    TradingLimitPolicy savePolicy(
            UUID userId,
            UpdateTradingLimitCommand command,
            BookingActor updatedBy
    );

    TradingLimitUsage usage(UUID userId, TradingLimitWindows windows);

    TradingLimitSnapshot snapshot(UUID userId, TradingLimitWindows windows);

    TradingLimitUserPage searchFoUsers(String query, int page, int size, TradingLimitWindows windows);

    boolean isActiveFoUser(UUID userId);
}

