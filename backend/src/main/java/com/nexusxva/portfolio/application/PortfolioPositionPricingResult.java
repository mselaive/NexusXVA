package com.nexusxva.portfolio.application;

import java.util.UUID;

public record PortfolioPositionPricingResult(
        UUID positionId,
        PortfolioPricingStatus status,
        String underlyingSymbol,
        double quantity,
        double unitPrice,
        double positionPrice,
        Double executionPrice,
        Double tradeValue,
        Double unrealizedPnl,
        PortfolioGreeks unitGreeks,
        PortfolioGreeks positionGreeks,
        PortfolioPositionMarketData marketData
) {
}
