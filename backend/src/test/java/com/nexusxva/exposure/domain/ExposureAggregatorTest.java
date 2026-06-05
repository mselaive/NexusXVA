package com.nexusxva.exposure.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

class ExposureAggregatorTest {

    private final ExposureAggregator aggregator = new ExposureAggregator();

    @Test
    void aggregatesExpectedExposureExpectedNegativeExposureAndPfe() {
        List<LocalDate> dates = List.of(
                LocalDate.parse("2026-07-01"),
                LocalDate.parse("2026-08-01")
        );
        double[][] portfolioValues = {
                {10.0, -5.0},
                {20.0, 30.0},
                {-10.0, -20.0},
                {40.0, 0.0}
        };

        List<ExposurePoint> points = aggregator.aggregate(dates, portfolioValues, 0.75);

        assertThat(points).hasSize(2);
        assertThat(points.get(0).expectedExposure()).isEqualTo(17.5);
        assertThat(points.get(0).expectedNegativeExposure()).isEqualTo(2.5);
        assertThat(points.get(0).pfe()).isEqualTo(20.0);
        assertThat(points.get(1).expectedExposure()).isEqualTo(7.5);
        assertThat(points.get(1).expectedNegativeExposure()).isEqualTo(6.25);
        assertThat(points.get(1).pfe()).isZero();
    }
}
