package com.nexusxva.portfolio.api;

import com.nexusxva.portfolio.application.PortfolioGreeks;

public record PortfolioGreeksResponse(
        double delta,
        double gamma,
        double vega,
        double theta,
        double rho
) {

    public static PortfolioGreeksResponse from(PortfolioGreeks greeks) {
        return new PortfolioGreeksResponse(
                greeks.delta(),
                greeks.gamma(),
                greeks.vega(),
                greeks.theta(),
                greeks.rho()
        );
    }
}
