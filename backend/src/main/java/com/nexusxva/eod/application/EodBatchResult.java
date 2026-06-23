package com.nexusxva.eod.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record EodBatchResult(
        LocalDate businessDate,
        int totalPortfolios,
        int captured,
        int skipped,
        int failed,
        Instant completedAt,
        List<EodBatchPortfolioResult> portfolios
) {
    public EodBatchResult {
        portfolios = List.copyOf(portfolios);
    }
}
