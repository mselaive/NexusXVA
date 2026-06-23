package com.nexusxva.eod.application;

import java.util.UUID;

public record EodBatchPortfolioResult(
        UUID portfolioId,
        String portfolioName,
        String status,
        String message
) {
}
