package com.nexusxva.portfolio.domain;

import java.time.Instant;
import java.util.UUID;

public record PortfolioSummary(
        UUID id,
        String name,
        String description,
        String baseCurrency,
        Instant createdAt,
        Instant updatedAt,
        Instant archivedAt,
        long positionCount
) {
    public PortfolioSummary(
            UUID id,
            String name,
            String description,
            String baseCurrency,
            Instant createdAt,
            Instant updatedAt,
            long positionCount
    ) {
        this(id, name, description, baseCurrency, createdAt, updatedAt, null, positionCount);
    }
}
