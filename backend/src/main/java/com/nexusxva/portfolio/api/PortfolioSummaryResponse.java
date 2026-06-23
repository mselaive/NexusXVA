package com.nexusxva.portfolio.api;

import com.nexusxva.portfolio.domain.PortfolioSummary;

import java.time.Instant;
import java.util.UUID;

public record PortfolioSummaryResponse(
        UUID id,
        String name,
        String description,
        String baseCurrency,
        Instant createdAt,
        Instant updatedAt,
        long positionCount
) {

    public static PortfolioSummaryResponse from(PortfolioSummary portfolio) {
        return new PortfolioSummaryResponse(
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
