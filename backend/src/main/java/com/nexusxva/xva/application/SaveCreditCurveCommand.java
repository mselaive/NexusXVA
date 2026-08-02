package com.nexusxva.xva.application;

import com.nexusxva.xva.domain.CreditCurve;
import com.nexusxva.xva.domain.CreditCurveType;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SaveCreditCurveCommand(
        UUID counterpartyId,
        String name,
        CreditCurveType curveType,
        boolean active,
        List<CreditCurve.Point> points,
        com.nexusxva.xva.domain.CurveSource source,
        Instant sourceAsOf,
        String sourceReference,
        String sourceSeriesId,
        String constructionMethod,
        boolean sourceStale,
        String sourceCurrency,
        String sourceCreditRating,
        String sourceRatingBucket,
        Double sourceRecoveryRate,
        Double sourceSpread,
        String sourceSpreadUnit,
        Double sourceHazardRate,
        LocalDate sourceObservationDate,
        boolean marketProxy
) {
    public SaveCreditCurveCommand(UUID counterpartyId, String name, CreditCurveType curveType, boolean active, List<CreditCurve.Point> points) {
        this(counterpartyId, name, curveType, active, points, com.nexusxva.xva.domain.CurveSource.MANUAL,
                null, null, null, null, false, null, null, null, null, null, null, null, null, false);
    }

    public SaveCreditCurveCommand(UUID counterpartyId, String name, CreditCurveType curveType, boolean active,
                                  List<CreditCurve.Point> points, com.nexusxva.xva.domain.CurveSource source) {
        this(counterpartyId, name, curveType, active, points, source,
                null, null, null, null, false, null, null, null, null, null, null, null, null, false);
    }

    public SaveCreditCurveCommand {
        if (counterpartyId == null) {
            throw new IllegalArgumentException("counterpartyId is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (curveType == null) {
            throw new IllegalArgumentException("curveType is required");
        }
        source = source == null ? com.nexusxva.xva.domain.CurveSource.MANUAL : source;
        sourceReference = normalize(sourceReference, 240, "sourceReference");
        sourceSeriesId = normalize(sourceSeriesId, 80, "sourceSeriesId");
        constructionMethod = normalize(constructionMethod, 120, "constructionMethod");
        sourceCurrency = normalize(sourceCurrency, 3, "sourceCurrency");
        sourceCreditRating = normalize(sourceCreditRating, 16, "sourceCreditRating");
        sourceRatingBucket = normalize(sourceRatingBucket, 8, "sourceRatingBucket");
        sourceSpreadUnit = normalize(sourceSpreadUnit, 16, "sourceSpreadUnit");
        points = points == null ? List.of() : points.stream()
                .sorted(java.util.Comparator.comparing(CreditCurve.Point::date))
                .toList();
        if (points.isEmpty()) {
            throw new IllegalArgumentException("credit curve must contain at least one point");
        }
        validatePoints(curveType, points);
    }

    private static String normalize(String value, int maxLength, String field) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        if (normalized.length() > maxLength) throw new IllegalArgumentException(field + " is too long");
        return normalized;
    }

    private static void validatePoints(CreditCurveType curveType, List<CreditCurve.Point> points) {
        LocalDate previousDate = null;
        Double previousValue = null;
        for (CreditCurve.Point point : points) {
            if (point.date() == null) {
                throw new IllegalArgumentException("credit curve point date is required");
            }
            if (point.date().equals(previousDate)) {
                throw new IllegalArgumentException("credit curve point dates must not contain duplicates");
            }
            previousDate = point.date();
            Double value = curveType == CreditCurveType.SURVIVAL_PROBABILITY
                    ? point.survivalProbability()
                    : point.cumulativeDefaultProbability();
            boolean hasSurvival = point.survivalProbability() != null;
            boolean hasCumulative = point.cumulativeDefaultProbability() != null;
            if (value == null || hasSurvival == hasCumulative) {
                throw new IllegalArgumentException("credit curve point must match curveType");
            }
            if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
                throw new IllegalArgumentException("credit curve point value must be between 0 and 1");
            }
            if (previousValue != null) {
                if (curveType == CreditCurveType.SURVIVAL_PROBABILITY && value > previousValue) {
                    throw new IllegalArgumentException("survivalProbability must not increase over time");
                }
                if (curveType == CreditCurveType.CUMULATIVE_DEFAULT_PROBABILITY && value < previousValue) {
                    throw new IllegalArgumentException("cumulativeDefaultProbability must not decrease over time");
                }
            }
            previousValue = value;
        }
    }
}
