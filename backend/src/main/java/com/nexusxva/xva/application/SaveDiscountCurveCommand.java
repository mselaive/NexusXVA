package com.nexusxva.xva.application;

import com.nexusxva.xva.domain.DiscountCurve;
import java.time.LocalDate;
import java.util.List;

public record SaveDiscountCurveCommand(
        String name,
        String currency,
        boolean active,
        List<DiscountCurve.Point> points,
        com.nexusxva.xva.domain.CurveSource source
) {
    public SaveDiscountCurveCommand(String name, String currency, boolean active, List<DiscountCurve.Point> points) {
        this(name, currency, active, points, com.nexusxva.xva.domain.CurveSource.MANUAL);
    }

    public SaveDiscountCurveCommand {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (currency == null || !currency.trim().toUpperCase().matches("^[A-Z]{3}$")) {
            throw new IllegalArgumentException("currency must be a 3-letter code");
        }
        currency = currency.trim().toUpperCase();
        source = source == null ? com.nexusxva.xva.domain.CurveSource.MANUAL : source;
        points = points == null ? List.of() : points.stream()
                .sorted(java.util.Comparator.comparing(DiscountCurve.Point::date))
                .toList();
        if (points.isEmpty()) {
            throw new IllegalArgumentException("discount curve must contain at least one point");
        }
        LocalDate previousDate = null;
        for (DiscountCurve.Point point : points) {
            if (point.date() == null) {
                throw new IllegalArgumentException("discount curve point date is required");
            }
            if (point.date().equals(previousDate)) {
                throw new IllegalArgumentException("discount curve point dates must not contain duplicates");
            }
            if (!Double.isFinite(point.discountFactor()) || point.discountFactor() < 0.0 || point.discountFactor() > 1.0) {
                throw new IllegalArgumentException("discountFactor must be between 0 and 1");
            }
            previousDate = point.date();
        }
    }
}
