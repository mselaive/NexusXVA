package com.nexusxva.cva.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexusxva.exposure.domain.ExposurePoint;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

class SimplifiedCvaCalculatorTest {

    private final SimplifiedCvaCalculator calculator = new SimplifiedCvaCalculator();

    @Test
    void calculatesSimplifiedCvaFromExpectedExposureAndDefaultProbabilityIncrements() {
        LocalDate valuationDate = LocalDate.parse("2026-06-05");
        CvaResult result = calculator.calculate(new CvaInput(
                valuationDate,
                List.of(
                        new ExposurePoint(valuationDate.plusDays(365), 100.0, 0.0, 120.0),
                        new ExposurePoint(valuationDate.plusDays(730), 80.0, 0.0, 100.0)
                ),
                0.60,
                0.02,
                0.05
        ));

        double survival1 = Math.exp(-0.02);
        double survival2 = Math.exp(-0.04);
        double expected = 0.60
                * (
                Math.exp(-0.05) * 100.0 * (1.0 - survival1)
                        + Math.exp(-0.10) * 80.0 * (survival1 - survival2)
        );

        assertThat(result.cva()).isCloseTo(expected, withinTolerance());
        assertThat(result.points()).hasSize(2);
        assertThat(result.points().getFirst().defaultProbabilityIncrement()).isCloseTo(1.0 - survival1, withinTolerance());
    }

    @Test
    void cvaIsZeroWhenExpectedExposureIsZero() {
        LocalDate valuationDate = LocalDate.parse("2026-06-05");
        CvaResult result = calculator.calculate(new CvaInput(
                valuationDate,
                List.of(new ExposurePoint(valuationDate.plusDays(365), 0.0, 0.0, 0.0)),
                0.60,
                0.02,
                0.05
        ));

        assertThat(result.cva()).isZero();
        assertThat(result.points().getFirst().cvaContribution()).isZero();
    }

    @Test
    void cvaIsZeroWhenLossGivenDefaultIsZero() {
        LocalDate valuationDate = LocalDate.parse("2026-06-05");
        CvaResult result = calculator.calculate(new CvaInput(
                valuationDate,
                List.of(new ExposurePoint(valuationDate.plusDays(365), 100.0, 0.0, 120.0)),
                0.0,
                0.02,
                0.05
        ));

        assertThat(result.cva()).isZero();
    }

    @Test
    void calculatesCvaWithSurvivalCreditCurveAndDiscountCurve() {
        LocalDate valuationDate = LocalDate.parse("2026-06-05");
        CvaResult result = calculator.calculate(new CvaInput(
                valuationDate,
                List.of(
                        new ExposurePoint(valuationDate.plusDays(365), 100.0, 0.0, 120.0),
                        new ExposurePoint(valuationDate.plusDays(730), 80.0, 0.0, 100.0)
                ),
                0.60,
                null,
                null,
                List.of(
                        new CreditCurvePoint(valuationDate.plusDays(365), 0.98, null),
                        new CreditCurvePoint(valuationDate.plusDays(730), 0.95, null)
                ),
                List.of(
                        new DiscountCurvePoint(valuationDate.plusDays(365), 0.95),
                        new DiscountCurvePoint(valuationDate.plusDays(730), 0.90)
                )
        ));

        double expected = 0.60 * (0.95 * 100.0 * 0.02 + 0.90 * 80.0 * 0.03);

        assertThat(result.creditMethod()).isEqualTo(CvaCreditMethod.CREDIT_CURVE);
        assertThat(result.discountMethod()).isEqualTo(CvaDiscountMethod.DISCOUNT_CURVE);
        assertThat(result.cva()).isCloseTo(expected, withinTolerance());
    }

    @Test
    void calculatesCvaWithCumulativeDefaultProbabilityCurve() {
        LocalDate valuationDate = LocalDate.parse("2026-06-05");
        CvaResult result = calculator.calculate(new CvaInput(
                valuationDate,
                List.of(
                        new ExposurePoint(valuationDate.plusDays(365), 100.0, 0.0, 120.0),
                        new ExposurePoint(valuationDate.plusDays(730), 80.0, 0.0, 100.0)
                ),
                0.60,
                null,
                0.0,
                List.of(
                        new CreditCurvePoint(valuationDate.plusDays(365), null, 0.02),
                        new CreditCurvePoint(valuationDate.plusDays(730), null, 0.05)
                ),
                List.of()
        ));

        double expected = 0.60 * (100.0 * 0.02 + 80.0 * 0.03);

        assertThat(result.cva()).isCloseTo(expected, withinTolerance());
    }

    @Test
    void interpolatesCreditAndDiscountCurveForIntermediateExposureDate() {
        LocalDate valuationDate = LocalDate.parse("2026-06-05");
        LocalDate firstCurveDate = valuationDate.plusDays(30);
        LocalDate exposureDate = valuationDate.plusDays(45);
        LocalDate secondCurveDate = valuationDate.plusDays(60);

        CvaResult result = calculator.calculate(new CvaInput(
                valuationDate,
                List.of(new ExposurePoint(exposureDate, 100.0, 0.0, 120.0)),
                0.60,
                null,
                null,
                List.of(
                        new CreditCurvePoint(firstCurveDate, 0.99, null),
                        new CreditCurvePoint(secondCurveDate, 0.97, null)
                ),
                List.of(
                        new DiscountCurvePoint(firstCurveDate, 0.98),
                        new DiscountCurvePoint(secondCurveDate, 0.96)
                )
        ));

        assertThat(result.points().getFirst().survivalProbability()).isCloseTo(0.98, withinTolerance());
        assertThat(result.points().getFirst().discountFactor()).isCloseTo(0.97, withinTolerance());
        assertThat(result.cva()).isCloseTo(0.60 * 0.97 * 100.0 * 0.02, withinTolerance());
    }

    @Test
    void cvaIncreasesWhenHazardRateIncreases() {
        LocalDate valuationDate = LocalDate.parse("2026-06-05");
        List<ExposurePoint> exposures = List.of(
                new ExposurePoint(valuationDate.plusDays(365), 100.0, 0.0, 120.0)
        );

        double lowHazardCva = calculator.calculate(new CvaInput(valuationDate, exposures, 0.60, 0.01, 0.05)).cva();
        double highHazardCva = calculator.calculate(new CvaInput(valuationDate, exposures, 0.60, 0.05, 0.05)).cva();

        assertThat(highHazardCva).isGreaterThan(lowHazardCva);
    }

    @Test
    void rejectsExposurePointOnOrBeforeValuationDate() {
        LocalDate valuationDate = LocalDate.parse("2026-06-05");

        assertThatThrownBy(() -> new CvaInput(
                valuationDate,
                List.of(new ExposurePoint(valuationDate, 100.0, 0.0, 120.0)),
                0.60,
                0.02,
                0.05
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("exposure point dates must be after valuationDate");
    }

    @Test
    void rejectsCreditCurveOutsideExposureDateRange() {
        LocalDate valuationDate = LocalDate.parse("2026-06-05");

        assertThatThrownBy(() -> calculator.calculate(new CvaInput(
                valuationDate,
                List.of(new ExposurePoint(valuationDate.plusDays(30), 100.0, 0.0, 120.0)),
                0.60,
                null,
                0.05,
                List.of(
                        new CreditCurvePoint(valuationDate.plusDays(60), 0.99, null),
                        new CreditCurvePoint(valuationDate.plusDays(90), 0.98, null)
                ),
                List.of()
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("creditCurve does not cover exposure date");
    }

    @Test
    void rejectsIncreasingSurvivalProbability() {
        LocalDate valuationDate = LocalDate.parse("2026-06-05");

        assertThatThrownBy(() -> new CvaInput(
                valuationDate,
                List.of(new ExposurePoint(valuationDate.plusDays(90), 100.0, 0.0, 120.0)),
                0.60,
                null,
                0.05,
                List.of(
                        new CreditCurvePoint(valuationDate.plusDays(30), 0.98, null),
                        new CreditCurvePoint(valuationDate.plusDays(90), 0.99, null)
                ),
                List.of()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("creditCurve survivalProbability must not increase over time");
    }

    @Test
    void rejectsDecreasingCumulativeDefaultProbability() {
        LocalDate valuationDate = LocalDate.parse("2026-06-05");

        assertThatThrownBy(() -> new CvaInput(
                valuationDate,
                List.of(new ExposurePoint(valuationDate.plusDays(90), 100.0, 0.0, 120.0)),
                0.60,
                null,
                0.05,
                List.of(
                        new CreditCurvePoint(valuationDate.plusDays(30), null, 0.05),
                        new CreditCurvePoint(valuationDate.plusDays(90), null, 0.02)
                ),
                List.of()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("creditCurve cumulativeDefaultProbability must not decrease over time");
    }

    private org.assertj.core.data.Offset<Double> withinTolerance() {
        return org.assertj.core.data.Offset.offset(1.0e-10);
    }
}
