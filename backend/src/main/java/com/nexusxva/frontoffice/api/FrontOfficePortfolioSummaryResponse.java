package com.nexusxva.frontoffice.api;

import com.nexusxva.portfolio.domain.PortfolioSummary;
import java.time.Instant;
import java.util.UUID;

public record FrontOfficePortfolioSummaryResponse(
        UUID id,
        String name,
        String description,
        String baseCurrency,
        Instant createdAt,
        Instant updatedAt,
        long positionCount
) {

    static FrontOfficePortfolioSummaryResponse from(PortfolioSummary portfolio) {
        return new FrontOfficePortfolioSummaryResponse(
                portfolio.id(),
                portfolio.name(),
                portfolio.description(),
                portfolio.baseCurrency(),
                portfolio.createdAt(),
                portfolio.updatedAt(),
                portfolio.positionCount()
        );
    }
}
