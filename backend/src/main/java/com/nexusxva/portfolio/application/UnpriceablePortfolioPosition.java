package com.nexusxva.portfolio.application;

import java.util.UUID;

public record UnpriceablePortfolioPosition(
        UUID positionId,
        PortfolioPricingStatus status,
        String reason
) {
}
