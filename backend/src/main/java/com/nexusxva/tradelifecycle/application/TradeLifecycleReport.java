package com.nexusxva.tradelifecycle.application;

import java.time.Instant;
import java.util.List;

public record TradeLifecycleReport(
        int total,
        int pendingValidation,
        int approved,
        int rejected,
        int amendments,
        int cancellations,
        Long averageReviewMinutes,
        Instant oldestPendingSubmittedAt,
        List<LifecycleAgingBucket> pendingAgingBuckets,
        List<LifecycleBreakdown> byPortfolio,
        List<LifecycleBreakdown> bySymbol
) {
}
