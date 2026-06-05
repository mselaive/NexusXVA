package com.nexusxva.exposure.domain;

import java.time.LocalDate;

public record ExposurePoint(
        LocalDate date,
        double expectedExposure,
        double expectedNegativeExposure,
        double pfe
) {

    public ExposurePoint {
        if (date == null) {
            throw new IllegalArgumentException("exposure date is required");
        }
        requireNonNegativeFinite("expectedExposure", expectedExposure);
        requireNonNegativeFinite("expectedNegativeExposure", expectedNegativeExposure);
        requireNonNegativeFinite("pfe", pfe);
    }

    private static void requireNonNegativeFinite(String field, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(field + " must be finite");
        }
        if (value < 0.0) {
            throw new IllegalArgumentException(field + " must be greater than or equal to zero");
        }
    }
}
