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
    }

    public record Point(LocalDate date, double discountFactor) {}
}
