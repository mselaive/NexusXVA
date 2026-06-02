package com.nexusxva.portfolio.api;

import com.nexusxva.portfolio.application.PortfolioPositionMarketData;

import java.time.Instant;

public record PortfolioPositionMarketDataResponse(
        double spot,
        double volatility,
        double riskFreeRate,
        double dividendYield,
        String currency,
        Instant asOf,
        String source,
        boolean stale
) {

    public static PortfolioPositionMarketDataResponse from(PortfolioPositionMarketData marketData) {
        return new PortfolioPositionMarketDataResponse(
                marketData.spot(),
                marketData.volatility(),
                marketData.riskFreeRate(),
                marketData.dividendYield(),
                marketData.currency(),
                marketData.asOf(),
                marketData.source(),
                marketData.stale()
        );
    }
}
