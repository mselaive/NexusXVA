package com.nexusxva.pricing.api;

import com.nexusxva.instruments.domain.OptionType;
import com.nexusxva.pricing.domain.BlackScholesInput;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record BlackScholesPricingRequest(
        @NotNull OptionType optionType,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) Double spot,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) Double strike,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) Double timeToMaturityYears,
        @NotNull Double riskFreeRate,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) Double volatility
) {

    BlackScholesInput toInput() {
        return new BlackScholesInput(
                optionType,
                spot,
                strike,
                timeToMaturityYears,
                riskFreeRate,
                volatility
        );
    }
}
