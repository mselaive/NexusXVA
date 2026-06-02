package com.nexusxva.pricing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexusxva.instruments.domain.OptionType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

class BlackScholesCalculatorTest {

    private static final double TOLERANCE = 1.0e-4;

    private final BlackScholesCalculator calculator = new BlackScholesCalculator();

    @Test
    void pricesAtTheMoneyCallWithKnownBlackScholesValue() {
        BlackScholesResult result = calculator.price(input(OptionType.CALL, 100.0, 100.0, 1.0, 0.05, 0.2));

        assertThat(result.price()).isCloseTo(10.4506, withinTolerance());
        assertThat(result.greeks().delta()).isCloseTo(0.6368, withinTolerance());
        assertThat(result.greeks().gamma()).isCloseTo(0.0188, withinTolerance());
        assertThat(result.greeks().vega()).isCloseTo(37.5240, withinTolerance());
        assertThat(result.greeks().theta()).isCloseTo(-6.4140, withinTolerance());
        assertThat(result.greeks().rho()).isCloseTo(53.2325, withinTolerance());
    }

    @Test
    void pricesAtTheMoneyPutWithKnownBlackScholesValue() {
        BlackScholesResult result = calculator.price(input(OptionType.PUT, 100.0, 100.0, 1.0, 0.05, 0.2));

        assertThat(result.price()).isCloseTo(5.5735, withinTolerance());
        assertThat(result.greeks().delta()).isCloseTo(-0.3632, withinTolerance());
        assertThat(result.greeks().gamma()).isCloseTo(0.0188, withinTolerance());
        assertThat(result.greeks().vega()).isCloseTo(37.5240, withinTolerance());
        assertThat(result.greeks().theta()).isCloseTo(-1.6579, withinTolerance());
        assertThat(result.greeks().rho()).isCloseTo(-41.8905, withinTolerance());
    }

    @Test
    void pricesAtTheMoneyCallWithContinuousDividendYield() {
        BlackScholesResult result = calculator.price(
                input(OptionType.CALL, 100.0, 100.0, 1.0, 0.05, 0.2, 0.02)
        );

        assertThat(result.price()).isCloseTo(9.2270, withinTolerance());
        assertThat(result.greeks().delta()).isCloseTo(0.5869, withinTolerance());
        assertThat(result.greeks().gamma()).isCloseTo(0.0190, withinTolerance());
        assertThat(result.greeks().vega()).isCloseTo(37.9012, withinTolerance());
        assertThat(result.greeks().theta()).isCloseTo(-5.0893, withinTolerance());
        assertThat(result.greeks().rho()).isCloseTo(49.4581, withinTolerance());
    }

    @Test
    void satisfiesPutCallParity() {
        BlackScholesInput callInput = input(OptionType.CALL, 100.0, 95.0, 1.5, 0.03, 0.25);
        BlackScholesInput putInput = input(OptionType.PUT, 100.0, 95.0, 1.5, 0.03, 0.25);

        double callPrice = calculator.price(callInput).price();
        double putPrice = calculator.price(putInput).price();
        double discountedStrike = putInput.strike() * Math.exp(-putInput.riskFreeRate() * putInput.timeToMaturityYears());

        assertThat(callPrice - putPrice).isCloseTo(putInput.spot() - discountedStrike, withinTolerance());
    }

    @Test
    void callPriceIncreasesWhenSpotIncreases() {
        double lowerSpotPrice = calculator.price(input(OptionType.CALL, 95.0, 100.0, 1.0, 0.05, 0.2)).price();
        double higherSpotPrice = calculator.price(input(OptionType.CALL, 105.0, 100.0, 1.0, 0.05, 0.2)).price();

        assertThat(higherSpotPrice).isGreaterThan(lowerSpotPrice);
    }

    @Test
    void callPriceIncreasesWhenVolatilityIncreases() {
        double lowerVolatilityPrice = calculator.price(input(OptionType.CALL, 100.0, 100.0, 1.0, 0.05, 0.1)).price();
        double higherVolatilityPrice = calculator.price(input(OptionType.CALL, 100.0, 100.0, 1.0, 0.05, 0.3)).price();

        assertThat(higherVolatilityPrice).isGreaterThan(lowerVolatilityPrice);
    }

    @Test
    void pricesAreNeverNegativeForValidInputs() {
        BlackScholesResult call = calculator.price(input(OptionType.CALL, 80.0, 100.0, 2.0, -0.01, 0.35));
        BlackScholesResult put = calculator.price(input(OptionType.PUT, 120.0, 100.0, 2.0, -0.01, 0.35));

        assertThat(call.price()).isGreaterThanOrEqualTo(0.0);
        assertThat(put.price()).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    void acceptsNegativeRiskFreeRateWhenResultRemainsFinite() {
        BlackScholesResult result = calculator.price(input(OptionType.CALL, 100.0, 100.0, 1.0, -0.01, 0.2));

        assertThat(result.price()).isPositive();
        assertThat(result.greeks().delta()).isBetween(0.0, 1.0);
    }

    @Test
    void rejectsNonFinitePricingResults() {
        BlackScholesInput extremeInput = input(OptionType.CALL, 100.0, 100.0, 1000.0, -1000.0, 0.2);

        assertThatThrownBy(() -> calculator.price(extremeInput))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be finite");
    }

    @ParameterizedTest
    @MethodSource("invalidInputs")
    void rejectsInvalidInputs(BlackScholesInputFactory factory) {
        assertThatThrownBy(factory::create)
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Stream<Arguments> invalidInputs() {
        return Stream.of(
                Arguments.of((BlackScholesInputFactory) () -> input(null, 100.0, 100.0, 1.0, 0.05, 0.2)),
                Arguments.of((BlackScholesInputFactory) () -> input(OptionType.CALL, 0.0, 100.0, 1.0, 0.05, 0.2)),
                Arguments.of((BlackScholesInputFactory) () -> input(OptionType.CALL, 100.0, 0.0, 1.0, 0.05, 0.2)),
                Arguments.of((BlackScholesInputFactory) () -> input(OptionType.CALL, 100.0, 100.0, 0.0, 0.05, 0.2)),
                Arguments.of((BlackScholesInputFactory) () -> input(OptionType.CALL, 100.0, 100.0, 1.0, Double.NaN, 0.2)),
                Arguments.of((BlackScholesInputFactory) () -> input(OptionType.CALL, 100.0, 100.0, 1.0, 0.05, Double.POSITIVE_INFINITY)),
                Arguments.of((BlackScholesInputFactory) () -> input(OptionType.CALL, 100.0, 100.0, 1.0, 0.05, -0.2))
        );
    }

    private static BlackScholesInput input(
            OptionType optionType,
            double spot,
            double strike,
            double timeToMaturityYears,
            double riskFreeRate,
            double volatility
    ) {
        return new BlackScholesInput(optionType, spot, strike, timeToMaturityYears, riskFreeRate, volatility);
    }

    private static BlackScholesInput input(
            OptionType optionType,
            double spot,
            double strike,
            double timeToMaturityYears,
            double riskFreeRate,
            double volatility,
            double dividendYield
    ) {
        return new BlackScholesInput(
                optionType,
                spot,
                strike,
                timeToMaturityYears,
                riskFreeRate,
                volatility,
                dividendYield
        );
    }

    private static org.assertj.core.data.Offset<Double> withinTolerance() {
        return org.assertj.core.data.Offset.offset(TOLERANCE);
    }

    @FunctionalInterface
    private interface BlackScholesInputFactory {
        BlackScholesInput create();
    }
}
