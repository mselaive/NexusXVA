package com.nexusxva.cva.domain;

import java.time.LocalDate;

public record DiscountCurvePoint(
        LocalDate date,
        double discountFactor
) {

    public DiscountCurvePoint {
        if (date == null) {
            throw new IllegalArgumentException("discountCurve date is required");
        }
        if (!Double.isFinite(discountFactor) || discountFactor < 0.0 || discountFactor > 1.0) {
            throw new IllegalArgumentException("discountFactor must be between 0 and 1");
        }
    }
}
