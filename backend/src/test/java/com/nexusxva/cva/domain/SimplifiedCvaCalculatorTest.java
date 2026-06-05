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

    private org.assertj.core.data.Offset<Double> withinTolerance() {
        return org.assertj.core.data.Offset.offset(1.0e-10);
    }
}
