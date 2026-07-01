package com.nexusxva.portfolio.application;

import java.time.Instant;

public record PortfolioPositionMarketData(
        double spot,
        double volatility,
        double riskFreeRate,
        double dividendYield,
        String currency,
        String baseCurrency,
        double fxRateToBase,
        Instant asOf,
        String source,
        boolean stale
) {
    public PortfolioPositionMarketData(
            double spot,
            double volatility,
            double riskFreeRate,
            double dividendYield,
            String currency,
            Instant asOf,
            String source,
            boolean stale
    ) {
        this(spot, volatility, riskFreeRate, dividendYield, currency, currency, 1.0, asOf, source, stale);
    }
}
