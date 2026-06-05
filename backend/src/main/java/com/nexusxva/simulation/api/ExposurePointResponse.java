package com.nexusxva.simulation.api;

import com.nexusxva.exposure.domain.ExposurePoint;

import java.time.LocalDate;

public record ExposurePointResponse(
        LocalDate date,
        double expectedExposure,
        double expectedNegativeExposure,
        double pfe
) {

    static ExposurePointResponse from(ExposurePoint point) {
        return new ExposurePointResponse(
                point.date(),
                point.expectedExposure(),
                point.expectedNegativeExposure(),
                point.pfe()
        );
    }
}
