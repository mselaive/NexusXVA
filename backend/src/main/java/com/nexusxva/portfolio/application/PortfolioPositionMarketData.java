package com.nexusxva.portfolio.application;

import java.time.Instant;

public record PortfolioPositionMarketData(
        double spot,
        double volatility,
        double riskFreeRate,
        double dividendYield,
        String currency,
        Instant asOf,
        String source,
        boolean stale
) {
}
