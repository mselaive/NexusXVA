package com.nexusxva.portfolio.application;

public record PortfolioGreeks(
        double delta,
        double gamma,
        double vega,
        double theta,
        double rho
) {

    public static PortfolioGreeks zero() {
        return new PortfolioGreeks(0.0, 0.0, 0.0, 0.0, 0.0);
    }

    public PortfolioGreeks plus(PortfolioGreeks other) {
        return new PortfolioGreeks(
                delta + other.delta(),
                gamma + other.gamma(),
                vega + other.vega(),
                theta + other.theta(),
                rho + other.rho()
        );
    }

    public static PortfolioGreeks scaled(
            com.nexusxva.pricing.domain.Greeks greeks,
            double quantity
    ) {
        return new PortfolioGreeks(
                greeks.delta() * quantity,
                greeks.gamma() * quantity,
                greeks.vega() * quantity,
                greeks.theta() * quantity,
                greeks.rho() * quantity
        );
    }

    public PortfolioGreeks monetaryGreeksConverted(double fxRateToBase) {
        return new PortfolioGreeks(
                delta,
                gamma * fxRateToBase,
                vega * fxRateToBase,
                theta * fxRateToBase,
                rho * fxRateToBase
        );
    }
}
