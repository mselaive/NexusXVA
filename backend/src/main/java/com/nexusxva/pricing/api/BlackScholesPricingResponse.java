package com.nexusxva.pricing.api;

import com.nexusxva.instruments.domain.OptionType;
import com.nexusxva.pricing.domain.BlackScholesResult;
import com.nexusxva.pricing.domain.Greeks;

public record BlackScholesPricingResponse(
        String model,
        OptionType optionType,
        double price,
        GreeksResponse greeks
) {

    static BlackScholesPricingResponse from(BlackScholesResult result) {
        return new BlackScholesPricingResponse(
                "BLACK_SCHOLES",
                result.optionType(),
                result.price(),
                GreeksResponse.from(result.greeks())
        );
    }

    public record GreeksResponse(
            double delta,
            double gamma,
            double vega,
            double theta,
            double rho
    ) {

        static GreeksResponse from(Greeks greeks) {
            return new GreeksResponse(
                    greeks.delta(),
                    greeks.gamma(),
                    greeks.vega(),
                    greeks.theta(),
                    greeks.rho()
            );
        }
    }
}
