package com.nexusxva.pricing.domain;

import com.nexusxva.instruments.domain.OptionType;

public class BlackScholesCalculator {

    private static final double INV_SQRT_TWO_PI = 1.0 / Math.sqrt(2.0 * Math.PI);

    public BlackScholesResult price(BlackScholesInput input) {
        double spot = input.spot();
        double strike = input.strike();
        double time = input.timeToMaturityYears();
        double rate = input.riskFreeRate();
        double volatility = input.volatility();
        double dividendYield = input.dividendYield();
        double sqrtTime = Math.sqrt(time);
        double d1 = d1(spot, strike, time, rate, volatility, dividendYield, sqrtTime);
        double d2 = d1 - volatility * sqrtTime;
        double discountFactor = Math.exp(-rate * time);
        double dividendDiscountFactor = Math.exp(-dividendYield * time);

        BlackScholesResult result = switch (input.optionType()) {
            case CALL -> callResult(
                    spot,
                    strike,
                    time,
                    rate,
                    volatility,
                    dividendYield,
                    sqrtTime,
                    d1,
                    d2,
                    discountFactor,
                    dividendDiscountFactor
            );
            case PUT -> putResult(
                    spot,
                    strike,
                    time,
                    rate,
                    volatility,
                    dividendYield,
                    sqrtTime,
                    d1,
                    d2,
                    discountFactor,
                    dividendDiscountFactor
            );
        };

        validateFiniteResult(result);
        return result;
    }

    private BlackScholesResult callResult(
            double spot,
            double strike,
            double time,
            double rate,
            double volatility,
            double dividendYield,
            double sqrtTime,
            double d1,
            double d2,
            double discountFactor,
            double dividendDiscountFactor
    ) {
        double price = spot * dividendDiscountFactor * cdf(d1) - strike * discountFactor * cdf(d2);
        Greeks greeks = new Greeks(
                dividendDiscountFactor * cdf(d1),
                gamma(spot, volatility, sqrtTime, d1, dividendDiscountFactor),
                vega(spot, sqrtTime, d1, dividendDiscountFactor),
                callTheta(
                        spot,
                        strike,
                        rate,
                        volatility,
                        dividendYield,
                        sqrtTime,
                        d1,
                        d2,
                        discountFactor,
                        dividendDiscountFactor
                ),
                strike * time * discountFactor * cdf(d2)
        );

        return new BlackScholesResult(OptionType.CALL, price, greeks);
    }

    private BlackScholesResult putResult(
            double spot,
            double strike,
            double time,
            double rate,
            double volatility,
            double dividendYield,
            double sqrtTime,
            double d1,
            double d2,
            double discountFactor,
            double dividendDiscountFactor
    ) {
        double price = strike * discountFactor * cdf(-d2) - spot * dividendDiscountFactor * cdf(-d1);
        Greeks greeks = new Greeks(
                dividendDiscountFactor * (cdf(d1) - 1.0),
                gamma(spot, volatility, sqrtTime, d1, dividendDiscountFactor),
                vega(spot, sqrtTime, d1, dividendDiscountFactor),
                putTheta(
                        spot,
                        strike,
                        rate,
                        volatility,
                        dividendYield,
                        sqrtTime,
                        d1,
                        d2,
                        discountFactor,
                        dividendDiscountFactor
                ),
                -strike * time * discountFactor * cdf(-d2)
        );

        return new BlackScholesResult(OptionType.PUT, price, greeks);
    }

    private double d1(
            double spot,
            double strike,
            double time,
            double rate,
            double volatility,
            double dividendYield,
            double sqrtTime
    ) {
        return (Math.log(spot / strike) + (rate - dividendYield + 0.5 * volatility * volatility) * time)
                / (volatility * sqrtTime);
    }

    private double gamma(double spot, double volatility, double sqrtTime, double d1, double dividendDiscountFactor) {
        return dividendDiscountFactor * pdf(d1) / (spot * volatility * sqrtTime);
    }

    private double vega(double spot, double sqrtTime, double d1, double dividendDiscountFactor) {
        return spot * dividendDiscountFactor * pdf(d1) * sqrtTime;
    }

    private double callTheta(
            double spot,
            double strike,
            double rate,
            double volatility,
            double dividendYield,
            double sqrtTime,
            double d1,
            double d2,
            double discountFactor,
            double dividendDiscountFactor
    ) {
        return -spot * dividendDiscountFactor * pdf(d1) * volatility / (2.0 * sqrtTime)
                - rate * strike * discountFactor * cdf(d2)
                + dividendYield * spot * dividendDiscountFactor * cdf(d1);
    }

    private double putTheta(
            double spot,
            double strike,
            double rate,
            double volatility,
            double dividendYield,
            double sqrtTime,
            double d1,
            double d2,
            double discountFactor,
            double dividendDiscountFactor
    ) {
        return -spot * dividendDiscountFactor * pdf(d1) * volatility / (2.0 * sqrtTime)
                + rate * strike * discountFactor * cdf(-d2)
                - dividendYield * spot * dividendDiscountFactor * cdf(-d1);
    }

    private double pdf(double value) {
        return INV_SQRT_TWO_PI * Math.exp(-0.5 * value * value);
    }

    private double cdf(double value) {
        double sign = value < 0.0 ? -1.0 : 1.0;
        double x = Math.abs(value) / Math.sqrt(2.0);
        double t = 1.0 / (1.0 + 0.3275911 * x);
        double erf = 1.0 - (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t
                - 0.284496736) * t + 0.254829592) * t * Math.exp(-x * x);
        return 0.5 * (1.0 + sign * erf);
    }

    private void validateFiniteResult(BlackScholesResult result) {
        requireFinite("price", result.price());
        requireFinite("delta", result.greeks().delta());
        requireFinite("gamma", result.greeks().gamma());
        requireFinite("vega", result.greeks().vega());
        requireFinite("theta", result.greeks().theta());
        requireFinite("rho", result.greeks().rho());
    }

    private void requireFinite(String field, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Black-Scholes " + field + " result must be finite");
        }
    }
}
