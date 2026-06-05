package com.nexusxva.cva.domain;

import com.nexusxva.exposure.domain.ExposurePoint;

import java.time.LocalDate;
import java.util.List;

public record CvaInput(
        LocalDate valuationDate,
        List<ExposurePoint> exposurePoints,
        double lossGivenDefault,
        double counterpartyHazardRate,
        double discountRate
) {

    public CvaInput {
        if (valuationDate == null) {
            throw new IllegalArgumentException("valuationDate is required");
        }
        if (exposurePoints == null) {
            throw new IllegalArgumentException("exposurePoints are required");
        }
        exposurePoints = List.copyOf(exposurePoints);
        for (ExposurePoint point : exposurePoints) {
            if (!point.date().isAfter(valuationDate)) {
                throw new IllegalArgumentException("exposure point dates must be after valuationDate");
            }
        }
        requireUnitInterval("lossGivenDefault", lossGivenDefault);
        requireNonNegativeFinite("counterpartyHazardRate", counterpartyHazardRate);
        if (!Double.isFinite(discountRate)) {
            throw new IllegalArgumentException("discountRate must be finite");
        }
    }

    private static void requireUnitInterval(String field, double value) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(field + " must be between 0 and 1");
        }
    }

    private static void requireNonNegativeFinite(String field, double value) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(field + " must be greater than or equal to zero");
        }
    }
}
