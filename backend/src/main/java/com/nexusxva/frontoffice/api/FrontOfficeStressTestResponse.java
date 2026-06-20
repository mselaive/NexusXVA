package com.nexusxva.frontoffice.api;

import com.nexusxva.frontoffice.application.FrontOfficeStressTestService.FrontOfficeStressTestResult;
import com.nexusxva.frontoffice.application.FrontOfficeStressTestService.StressImpact;
import com.nexusxva.frontoffice.application.FrontOfficeStressTestService.StressPortfolioTotals;
import com.nexusxva.frontoffice.application.FrontOfficeStressTestService.StressScenario;
import com.nexusxva.frontoffice.application.FrontOfficeStressTestService.StressScenarioResult;
import com.nexusxva.portfolio.api.PortfolioGreeksResponse;
import com.nexusxva.portfolio.api.PortfolioPositionPricingResponse;
import com.nexusxva.portfolio.api.UnpriceablePortfolioPositionResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record FrontOfficeStressTestResponse(
        UUID portfolioId,
        LocalDate valuationDate,
        String model,
        String baseCurrency,
        StressPortfolioTotalsResponse basePortfolio,
        PortfolioPositionPricingResponse hypotheticalTrade,
        List<StressScenarioResultResponse> scenarios,
        List<UnpriceablePortfolioPositionResponse> unpriceablePositions
) {

    static FrontOfficeStressTestResponse from(FrontOfficeStressTestResult result) {
        return new FrontOfficeStressTestResponse(
                result.portfolioId(),
                result.valuationDate(),
                result.model(),
                result.baseCurrency(),
                StressPortfolioTotalsResponse.from(result.basePortfolio()),
                result.hypotheticalTrade() == null ? null : PortfolioPositionPricingResponse.from(result.hypotheticalTrade()),
                result.scenarios().stream()
                        .map(StressScenarioResultResponse::from)
                        .toList(),
                result.unpriceablePositions().stream()
                        .map(UnpriceablePortfolioPositionResponse::from)
                        .toList()
        );
    }

    public record StressScenarioResponse(
            String name,
            double spotShockPercent,
            double volatilityShockBps,
            double riskFreeRateShockBps,
            double dividendYieldShockBps
    ) {

        static StressScenarioResponse from(StressScenario scenario) {
            return new StressScenarioResponse(
                    scenario.name(),
                    scenario.spotShockPercent(),
                    scenario.volatilityShockBps(),
                    scenario.riskFreeRateShockBps(),
                    scenario.dividendYieldShockBps()
            );
        }
    }

    public record StressScenarioResultResponse(
            StressScenarioResponse scenario,
            StressPortfolioTotalsResponse totals,
            StressImpactResponse impact,
            List<PortfolioPositionPricingResponse> positions
    ) {

        static StressScenarioResultResponse from(StressScenarioResult result) {
            return new StressScenarioResultResponse(
                    StressScenarioResponse.from(result.scenario()),
                    StressPortfolioTotalsResponse.from(result.totals()),
                    StressImpactResponse.from(result.impact()),
                    result.positions().stream()
                            .map(PortfolioPositionPricingResponse::from)
                            .toList()
            );
        }
    }

    public record StressPortfolioTotalsResponse(
            double totalPrice,
            PortfolioGreeksResponse totalGreeks
    ) {

        static StressPortfolioTotalsResponse from(StressPortfolioTotals totals) {
            return new StressPortfolioTotalsResponse(
                    totals.totalPrice(),
                    PortfolioGreeksResponse.from(totals.totalGreeks())
            );
        }
    }

    public record StressImpactResponse(
            double price,
            double delta,
            double gamma,
            double vega,
            double theta,
            double rho
    ) {

        static StressImpactResponse from(StressImpact impact) {
            return new StressImpactResponse(
                    impact.price(),
                    impact.delta(),
                    impact.gamma(),
                    impact.vega(),
                    impact.theta(),
                    impact.rho()
            );
        }
    }
}
