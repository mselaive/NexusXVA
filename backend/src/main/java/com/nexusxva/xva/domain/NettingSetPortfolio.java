package com.nexusxva.xva.domain;

import java.time.Instant;
import java.util.UUID;

public record NettingSetPortfolio(
        UUID portfolioId,
        String portfolioName,
        String baseCurrency,
        Instant assignedAt
) {
}
