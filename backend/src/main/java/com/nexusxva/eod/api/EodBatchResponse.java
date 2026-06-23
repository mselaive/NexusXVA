package com.nexusxva.eod.api;

import com.nexusxva.eod.application.EodBatchResult;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record EodBatchResponse(
        LocalDate businessDate,
        int totalPortfolios,
        int captured,
        int skipped,
        int failed,
        Instant completedAt,
        List<EodBatchPortfolioResponse> portfolios
) {
    static EodBatchResponse from(EodBatchResult result) {
        return new EodBatchResponse(
                result.businessDate(),
                result.totalPortfolios(),
                result.captured(),
                result.skipped(),
                result.failed(),
                result.completedAt(),
                result.portfolios().stream().map(EodBatchPortfolioResponse::from).toList()
        );
    }
}
