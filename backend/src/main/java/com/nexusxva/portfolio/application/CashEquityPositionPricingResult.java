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
        Double averageCost,
        Double costBasis,
        Double tradeValue,
        Double unrealizedPnl,
        double realizedPnl,
        PortfolioGreeks positionGreeks,
        PortfolioPositionMarketData marketData
) {
}
