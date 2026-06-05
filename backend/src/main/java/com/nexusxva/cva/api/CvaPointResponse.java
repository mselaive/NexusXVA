package com.nexusxva.cva.api;

import com.nexusxva.cva.domain.CvaPoint;

import java.time.LocalDate;

public record CvaPointResponse(
        LocalDate date,
        double expectedExposure,
        double discountFactor,
        double survivalProbability,
        double defaultProbabilityIncrement,
        double discountedExpectedExposure,
        double cvaContribution
) {

    static CvaPointResponse from(CvaPoint point) {
        return new CvaPointResponse(
                point.date(),
                point.expectedExposure(),
                point.discountFactor(),
                point.survivalProbability(),
                point.defaultProbabilityIncrement(),
                point.discountedExpectedExposure(),
                point.cvaContribution()
        );
    }
}
