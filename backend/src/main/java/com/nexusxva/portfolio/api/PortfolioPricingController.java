package com.nexusxva.portfolio.api;

import com.nexusxva.auth.application.UserAccessService;
import com.nexusxva.portfolio.application.PortfolioBlackScholesPricingService;

import jakarta.servlet.http.HttpServletRequest;
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
    private final UserAccessService userAccessService;

    public PortfolioPricingController(
            PortfolioBlackScholesPricingService pricingService,
            UserAccessService userAccessService
    ) {
        this.pricingService = pricingService;
        this.userAccessService = userAccessService;
    }

    @PostMapping("/{portfolioId}/pricing/black-scholes")
    public PortfolioBlackScholesPricingResponse priceWithBlackScholes(
            @PathVariable UUID portfolioId,
            @RequestBody(required = false) PortfolioBlackScholesPricingRequest request,
            HttpServletRequest servletRequest
    ) {
        userAccessService.requirePortfolioAccess(servletRequest, portfolioId);
        LocalDate valuationDate = request == null ? null : request.valuationDate();
        return PortfolioBlackScholesPricingResponse.from(pricingService.price(portfolioId, valuationDate));
    }
}
