package com.nexusxva.admin.api;

import java.util.UUID;

public record AdminPortfolioSummaryResponse(
        UUID id,
        String name,
        String baseCurrency,
        long positionCount
) {
}
