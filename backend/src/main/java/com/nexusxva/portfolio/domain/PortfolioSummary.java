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
        long positionCount
) {
}
