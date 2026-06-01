package com.nexusxva.pricing.application;

import com.nexusxva.pricing.domain.BlackScholesCalculator;
import com.nexusxva.pricing.domain.BlackScholesInput;
import com.nexusxva.pricing.domain.BlackScholesResult;

import org.springframework.stereotype.Service;

@Service
public class EuropeanOptionPricingService {

    private final BlackScholesCalculator calculator = new BlackScholesCalculator();

    public BlackScholesResult priceWithBlackScholes(BlackScholesInput input) {
        return calculator.price(input);
    }
}
