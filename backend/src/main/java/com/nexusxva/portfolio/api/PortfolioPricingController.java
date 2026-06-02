package com.nexusxva.portfolio.api;

import com.nexusxva.portfolio.application.PortfolioBlackScholesPricingService;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/portfolios")
public class PortfolioPricingController {

    private final PortfolioBlackScholesPricingService pricingService;

    public PortfolioPricingController(PortfolioBlackScholesPricingService pricingService) {
        this.pricingService = pricingService;
    }

    @PostMapping("/{portfolioId}/pricing/black-scholes")
    public PortfolioBlackScholesPricingResponse priceWithBlackScholes(
            @PathVariable UUID portfolioId,
            @RequestBody(required = false) PortfolioBlackScholesPricingRequest request
    ) {
        LocalDate valuationDate = request == null ? null : request.valuationDate();
        return PortfolioBlackScholesPricingResponse.from(pricingService.price(portfolioId, valuationDate));
    }
}
