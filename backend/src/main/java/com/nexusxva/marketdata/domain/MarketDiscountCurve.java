package com.nexusxva.marketdata.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record MarketDiscountCurve(
        String name,
        String currency,
        LocalDate valuationDate,
        Instant asOf,
        String source,
        String method,
        boolean stale,
        List<Point> points
) {
    public MarketDiscountCurve {
        points = points == null ? List.of() : List.copyOf(points);
        if (name == null || name.isBlank() || currency == null || !currency.matches("^[A-Z]{3}$")) {
            throw new IllegalArgumentException("Market discount curve identity is incomplete");
        }
        if (valuationDate == null || asOf == null || source == null || source.isBlank() || method == null || method.isBlank()) {
            throw new IllegalArgumentException("Market discount curve metadata is incomplete");
        }
        if (points.isEmpty()) {
            throw new IllegalArgumentException("Market discount curve contains no points");
        }
        LocalDate previousDate = valuationDate;
        double previousFactor = 1.0;
        for (Point point : points) {
            if (point.date() == null || !point.date().isAfter(previousDate)) {
                throw new IllegalArgumentException("Market discount curve dates must be strictly increasing after valuationDate");
            }
            if (!Double.isFinite(point.discountFactor()) || point.discountFactor() <= 0.0 || point.discountFactor() > 1.0) {
                throw new IllegalArgumentException("Market discount factors must be finite and in (0, 1]");
            }
            if (point.discountFactor() > previousFactor) {
                throw new IllegalArgumentException("Market discount factors must not increase");
            }
            previousDate = point.date();
            previousFactor = point.discountFactor();
        }
    }

    public record Point(LocalDate date, double discountFactor) {}
}
