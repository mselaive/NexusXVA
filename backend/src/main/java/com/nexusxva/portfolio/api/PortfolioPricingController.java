package com.nexusxva.portfolio.api;

import com.nexusxva.auth.application.UserAccessService;
import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.infrastructure.AuthSessionFilter;
import com.nexusxva.portfolio.application.PortfolioBlackScholesPricingResult;
import com.nexusxva.portfolio.application.PortfolioBlackScholesPricingService;
import com.nexusxva.shared.error.ResourceNotFoundException;
import com.nexusxva.valuationruns.application.ValuationRunService;
import com.nexusxva.valuationruns.domain.ValuationRunType;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/portfolios")
public class PortfolioPricingController {

    private final PortfolioBlackScholesPricingService pricingService;
    private final UserAccessService userAccessService;
    private final ValuationRunService valuationRunService;

    public PortfolioPricingController(
            PortfolioBlackScholesPricingService pricingService,
            UserAccessService userAccessService,
            ValuationRunService valuationRunService
    ) {
        this.pricingService = pricingService;
        this.userAccessService = userAccessService;
        this.valuationRunService = valuationRunService;
    }

    @PostMapping("/{portfolioId}/pricing/black-scholes")
    public PortfolioBlackScholesPricingResponse priceWithBlackScholes(
            @PathVariable UUID portfolioId,
            @RequestBody(required = false) PortfolioBlackScholesPricingRequest request,
            HttpServletRequest servletRequest
    ) {
        userAccessService.requirePortfolioAccess(servletRequest, portfolioId);
        LocalDate valuationDate = request == null ? null : request.valuationDate();
        Map<String, Object> input = pricingInput(portfolioId, valuationDate);
        try {
            PortfolioBlackScholesPricingResult result = pricingService.price(portfolioId, valuationDate);
            PortfolioBlackScholesPricingResponse response = PortfolioBlackScholesPricingResponse.from(result);
            valuationRunService.recordSuccess(
                    currentSession(servletRequest),
                    portfolioId,
                    ValuationRunType.PRICING,
                    result.model(),
                    result.valuationDate(),
                    input,
                    response,
                    pricingSummary(response)
            );
            return response;
        } catch (RuntimeException exception) {
            if (!(exception instanceof ResourceNotFoundException)) {
                valuationRunService.recordFailure(
                        currentSession(servletRequest),
                        portfolioId,
                        ValuationRunType.PRICING,
                        "BLACK_SCHOLES_PORTFOLIO_PRICING_V1",
                        valuationDate,
                        input,
                        exception
                );
            }
            throw exception;
        }
    }

    private Map<String, Object> pricingInput(UUID portfolioId, LocalDate valuationDate) {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("portfolioId", portfolioId);
        input.put("valuationDate", valuationDate);
        return input;
    }

    private Map<String, Object> pricingSummary(PortfolioBlackScholesPricingResponse response) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalPrice", response.totalPrice());
        summary.put("totalTradeValue", response.totalTradeValue());
        summary.put("totalUnrealizedPnl", response.totalUnrealizedPnl());
        summary.put("baseCurrency", response.baseCurrency());
        summary.put("pricedPositions", response.positions().size());
        summary.put("unpriceablePositions", response.unpriceablePositions().size());
        return summary;
    }

    private AuthSession currentSession(HttpServletRequest request) {
        Object value = request.getAttribute(AuthSessionFilter.SESSION_ATTRIBUTE);
        return value instanceof AuthSession session ? session : null;
    }
}
