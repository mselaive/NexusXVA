package com.nexusxva.tradinglimits.api;

import com.nexusxva.tradinglimits.application.TradingLimitUserPage;
import java.util.List;

public record TradingLimitUserPageResponse(
        List<TradingLimitSnapshotResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    static TradingLimitUserPageResponse from(TradingLimitUserPage result) {
        return new TradingLimitUserPageResponse(
                result.items().stream().map(TradingLimitSnapshotResponse::from).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages()
        );
    }
}

