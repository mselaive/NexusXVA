package com.nexusxva.portfolio.application;

import java.util.UUID;

public record CashEquityPositionPricingResult(
        UUID positionId,
        PortfolioPricingStatus status,
        String underlyingSymbol,
        double quantity,
        double spot,
        double marketValue,
        Double executionPrice,
        Double tradeValue,
        Double unrealizedPnl,
        PortfolioGreeks positionGreeks,
        PortfolioPositionMarketData marketData
) {
}
