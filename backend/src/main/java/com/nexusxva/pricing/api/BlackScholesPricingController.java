package com.nexusxva.pricing.api;

import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.infrastructure.AuthSessionFilter;
import com.nexusxva.operationalcontrol.application.OperationalControlService;
import com.nexusxva.pricing.application.EuropeanOptionPricingService;
import com.nexusxva.pricing.domain.BlackScholesResult;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pricing/european-options/black-scholes")
public class BlackScholesPricingController {

    private final EuropeanOptionPricingService pricingService;
    private final OperationalControlService operationalControlService;

    public BlackScholesPricingController(EuropeanOptionPricingService pricingService, OperationalControlService operationalControlService) {
        this.pricingService = pricingService;
        this.operationalControlService = operationalControlService;
    }

    @PostMapping
    public BlackScholesPricingResponse price(
            @Valid @RequestBody BlackScholesPricingRequest request,
            HttpServletRequest servletRequest
    ) {
        operationalControlService.ensureOpen("RUN_STATELESS_BLACK_SCHOLES", currentSession(servletRequest), servletRequest);
        BlackScholesResult result = pricingService.priceWithBlackScholes(request.toInput());
        return BlackScholesPricingResponse.from(result);
    }

    private AuthSession currentSession(HttpServletRequest request) {
        Object value = request.getAttribute(AuthSessionFilter.SESSION_ATTRIBUTE);
        return value instanceof AuthSession session ? session : null;
    }
}
