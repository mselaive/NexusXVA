package com.nexusxva.xva.api;

import com.nexusxva.xva.domain.NettingSetPortfolio;
import java.time.Instant;
import java.util.UUID;

public record NettingSetPortfolioResponse(
        UUID portfolioId,
        String portfolioName,
        String baseCurrency,
        Instant assignedAt
) {

    static NettingSetPortfolioResponse from(NettingSetPortfolio portfolio) {
        return new NettingSetPortfolioResponse(
                portfolio.portfolioId(),
                portfolio.portfolioName(),
                portfolio.baseCurrency(),
                portfolio.assignedAt()
        );
    }
}
