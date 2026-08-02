package com.nexusxva.marketdata.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record MarketCreditCurve(
        String name,
        String curveType,
        String creditRating,
        String ratingBucket,
        String currency,
        LocalDate valuationDate,
        double recoveryRate,
        double spread,
        String spreadUnit,
        double hazardRate,
        LocalDate observationDate,
        Instant asOf,
        String source,
        String sourceSeriesId,
        String method,
        boolean marketProxy,
        boolean stale,
        List<Point> points
) {
    public MarketCreditCurve {
        requireText(name, "name");
        if (!"CUMULATIVE_DEFAULT_PROBABILITY".equals(curveType)) {
            throw new IllegalArgumentException("Unsupported market credit curve type");
        }
        requireText(creditRating, "creditRating");
        requireText(ratingBucket, "ratingBucket");
        requireText(currency, "currency");
        if (valuationDate == null || observationDate == null || asOf == null) {
            throw new IllegalArgumentException("Market credit curve dates are incomplete");
        }
        if (!Double.isFinite(recoveryRate) || recoveryRate < 0.0 || recoveryRate >= 1.0) {
            throw new IllegalArgumentException("Invalid market credit curve recovery rate");
        }
        if (!Double.isFinite(spread) || spread < 0.0 || !Double.isFinite(hazardRate) || hazardRate < 0.0) {
            throw new IllegalArgumentException("Invalid market credit curve economics");
        }
        requireText(spreadUnit, "spreadUnit");
        requireText(source, "source");
        requireText(sourceSeriesId, "sourceSeriesId");
        requireText(method, "method");
        points = points == null ? List.of() : List.copyOf(points);
        if (points.size() != 7) {
            throw new IllegalArgumentException("Market credit curve must contain seven points");
        }
        LocalDate previousDate = null;
        double previousProbability = -1.0;
        for (Point point : points) {
            if (point.date() == null || !point.date().isAfter(valuationDate)
                    || previousDate != null && !point.date().isAfter(previousDate)) {
                throw new IllegalArgumentException("Market credit curve dates are invalid");
            }
            if (!Double.isFinite(point.cumulativeDefaultProbability())
                    || point.cumulativeDefaultProbability() < 0.0
                    || point.cumulativeDefaultProbability() > 1.0
                    || point.cumulativeDefaultProbability() < previousProbability) {
                throw new IllegalArgumentException("Market credit curve probabilities are invalid");
            }
            previousDate = point.date();
            previousProbability = point.cumulativeDefaultProbability();
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Market credit curve " + field + " is required");
        }
    }

    public record Point(LocalDate date, double cumulativeDefaultProbability) {}
}
