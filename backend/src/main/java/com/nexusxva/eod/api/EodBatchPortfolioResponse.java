package com.nexusxva.eod.api;

import com.nexusxva.eod.application.EodBatchPortfolioResult;
import java.util.UUID;

public record EodBatchPortfolioResponse(
        UUID portfolioId,
        String portfolioName,
        String status,
        String message
) {
    static EodBatchPortfolioResponse from(EodBatchPortfolioResult result) {
        return new EodBatchPortfolioResponse(
                result.portfolioId(),
                result.portfolioName(),
                result.status(),
                result.message()
        );
    }
}
