package com.nexusxva.tradelifecycle.api;

import com.nexusxva.tradelifecycle.domain.TradeLifecycleRequest;
import java.util.List;
import org.springframework.data.domain.Page;

public record TradeLifecyclePageResponse(
        List<TradeLifecycleResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    static TradeLifecyclePageResponse from(Page<TradeLifecycleRequest> page) {
        return new TradeLifecyclePageResponse(
                page.getContent().stream().map(TradeLifecycleResponse::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
