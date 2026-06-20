package com.nexusxva.frontoffice.api;

import com.nexusxva.frontoffice.application.FrontOfficeWhatIfService.FrontOfficeWhatIfResult;
import com.nexusxva.frontoffice.application.FrontOfficeWhatIfService.WhatIfImpact;
import com.nexusxva.frontoffice.application.FrontOfficeWhatIfService.WhatIfPortfolioTotals;
import com.nexusxva.portfolio.api.PortfolioGreeksResponse;
import com.nexusxva.portfolio.api.PortfolioPositionPricingResponse;
import java.time.LocalDate;
import java.util.UUID;

public record FrontOfficeWhatIfResponse(
        UUID portfolioId,
        LocalDate valuationDate,
        String model,
        WhatIfPortfolioTotalsResponse basePortfolio,
        PortfolioPositionPricingResponse hypotheticalTrade,
        WhatIfPortfolioTotalsResponse withTradePortfolio,
        WhatIfImpactResponse impact
) {

    static FrontOfficeWhatIfResponse from(FrontOfficeWhatIfResult result) {
        return new FrontOfficeWhatIfResponse(
                result.portfolioId(),
                result.valuationDate(),
                result.model(),
                WhatIfPortfolioTotalsResponse.from(result.basePortfolio()),
                PortfolioPositionPricingResponse.from(result.hypotheticalTrade()),
                WhatIfPortfolioTotalsResponse.from(result.withTradePortfolio()),
                WhatIfImpactResponse.from(result.impact())
        );
    }

    public record WhatIfPortfolioTotalsResponse(
            double totalPrice,
            PortfolioGreeksResponse totalGreeks
    ) {

        static WhatIfPortfolioTotalsResponse from(WhatIfPortfolioTotals totals) {
            return new WhatIfPortfolioTotalsResponse(
                    totals.totalPrice(),
                    PortfolioGreeksResponse.from(totals.totalGreeks())
            );
        }
    }

    public record WhatIfImpactResponse(
            double price,
            double delta,
            double gamma,
            double vega,
            double theta,
            double rho
    ) {

        static WhatIfImpactResponse from(WhatIfImpact impact) {
            return new WhatIfImpactResponse(
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
