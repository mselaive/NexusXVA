package com.nexusxva.pricing.api;

import com.nexusxva.pricing.application.EuropeanOptionPricingService;
import com.nexusxva.pricing.domain.BlackScholesResult;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pricing/european-options/black-scholes")
public class BlackScholesPricingController {

    private final EuropeanOptionPricingService pricingService;

    public BlackScholesPricingController(EuropeanOptionPricingService pricingService) {
        this.pricingService = pricingService;
    }

    @PostMapping
    public BlackScholesPricingResponse price(@Valid @RequestBody BlackScholesPricingRequest request) {
        BlackScholesResult result = pricingService.priceWithBlackScholes(request.toInput());
        return BlackScholesPricingResponse.from(result);
    }
}
