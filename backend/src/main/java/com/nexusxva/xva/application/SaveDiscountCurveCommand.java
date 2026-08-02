package com.nexusxva.xva.application;

import com.nexusxva.xva.domain.DiscountCurve;
import java.time.LocalDate;
import java.util.List;

public record SaveDiscountCurveCommand(
        String name,
        String currency,
        boolean active,
        List<DiscountCurve.Point> points,
        com.nexusxva.xva.domain.CurveSource source,
        java.time.Instant sourceAsOf,
        String sourceReference,
        String constructionMethod,
        boolean sourceStale
) {
    public SaveDiscountCurveCommand(String name, String currency, boolean active, List<DiscountCurve.Point> points) {
        this(name, currency, active, points, com.nexusxva.xva.domain.CurveSource.MANUAL, null, null, null, false);
    }

    public SaveDiscountCurveCommand(String name, String currency, boolean active, List<DiscountCurve.Point> points, com.nexusxva.xva.domain.CurveSource source) {
        this(name, currency, active, points, source, null, null, null, false);
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
        sourceReference = normalize(sourceReference, 240, "sourceReference");
        constructionMethod = normalize(constructionMethod, 120, "constructionMethod");
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

    private static String normalize(String value, int maxLength, String field) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        if (normalized.length() > maxLength) throw new IllegalArgumentException(field + " is too long");
        return normalized;
    }
}
