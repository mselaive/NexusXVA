package com.nexusxva.frontoffice.application;

import com.nexusxva.portfolio.application.AddEuropeanOptionPositionCommand;
import com.nexusxva.portfolio.application.PortfolioBlackScholesPricingResult;
import com.nexusxva.portfolio.application.PortfolioBlackScholesPricingService;
import com.nexusxva.portfolio.application.PortfolioGreeks;
import com.nexusxva.portfolio.application.PortfolioPositionPricingResult;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FrontOfficeWhatIfService {

    private static final String MODEL = "BLACK_SCHOLES_PRE_TRADE_WHAT_IF_V1";

    private final PortfolioBlackScholesPricingService pricingService;

    public FrontOfficeWhatIfService(PortfolioBlackScholesPricingService pricingService) {
        this.pricingService = pricingService;
    }

    @Transactional(readOnly = true)
    public FrontOfficeWhatIfResult run(
            UUID portfolioId,
            LocalDate valuationDate,
            AddEuropeanOptionPositionCommand trade
    ) {
        PortfolioBlackScholesPricingResult base = pricingService.price(portfolioId, valuationDate);
        PortfolioPositionPricingResult hypotheticalTrade = pricingService.priceHypotheticalPosition(
                portfolioId,
                trade,
                base.valuationDate()
        );
        return new FrontOfficeWhatIfResult(
                portfolioId,
                base.valuationDate(),
                MODEL,
                new WhatIfPortfolioTotals(base.totalPrice(), base.totalGreeks()),
                hypotheticalTrade,
                new WhatIfPortfolioTotals(
                        base.totalPrice() + hypotheticalTrade.positionPrice(),
                        base.totalGreeks().plus(hypotheticalTrade.positionGreeks())
                ),
                new WhatIfImpact(
                        hypotheticalTrade.positionPrice(),
                        hypotheticalTrade.positionGreeks().delta(),
                        hypotheticalTrade.positionGreeks().gamma(),
                        hypotheticalTrade.positionGreeks().vega(),
                        hypotheticalTrade.positionGreeks().theta(),
                        hypotheticalTrade.positionGreeks().rho()
                )
        );
    }

    public record FrontOfficeWhatIfResult(
            UUID portfolioId,
            LocalDate valuationDate,
            String model,
            WhatIfPortfolioTotals basePortfolio,
            PortfolioPositionPricingResult hypotheticalTrade,
            WhatIfPortfolioTotals withTradePortfolio,
            WhatIfImpact impact
    ) {
    }

    public record WhatIfPortfolioTotals(
            double totalPrice,
            PortfolioGreeks totalGreeks
    ) {
    }

    public record WhatIfImpact(
            double price,
            double delta,
            double gamma,
            double vega,
            double theta,
            double rho
    ) {
    }
}
