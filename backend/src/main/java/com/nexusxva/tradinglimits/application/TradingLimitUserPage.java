package com.nexusxva.tradinglimits.application;

import com.nexusxva.tradinglimits.domain.TradingLimitSnapshot;
import java.util.List;

public record TradingLimitUserPage(
        List<TradingLimitSnapshot> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}

