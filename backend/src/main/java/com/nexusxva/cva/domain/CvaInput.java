package com.nexusxva.cva.domain;

import com.nexusxva.exposure.domain.ExposurePoint;

import java.time.LocalDate;
import java.util.List;

public record CvaInput(
        LocalDate valuationDate,
        List<ExposurePoint> exposurePoints,
        double lossGivenDefault,
        Double counterpartyHazardRate,
        Double discountRate,
        List<CreditCurvePoint> creditCurve,
        List<DiscountCurvePoint> discountCurve
) {

    public CvaInput(
            LocalDate valuationDate,
            List<ExposurePoint> exposurePoints,
            double lossGivenDefault,
            double counterpartyHazardRate,
            double discountRate
    ) {
        this(valuationDate, exposurePoints, lossGivenDefault, counterpartyHazardRate, discountRate, List.of(), List.of());
    }

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
        creditCurve = creditCurve == null ? List.of() : List.copyOf(creditCurve);
        discountCurve = discountCurve == null ? List.of() : List.copyOf(discountCurve);
        if (creditCurve.isEmpty()) {
            if (counterpartyHazardRate == null) {
                throw new IllegalArgumentException("counterpartyHazardRate is required when creditCurve is not provided");
            }
            requireNonNegativeFinite("counterpartyHazardRate", counterpartyHazardRate);
        }
        if (discountCurve.isEmpty()) {
            if (discountRate == null) {
                throw new IllegalArgumentException("discountRate is required when discountCurve is not provided");
            }
            if (!Double.isFinite(discountRate)) {
                throw new IllegalArgumentException("discountRate must be finite");
            }
        }
        validateCreditCurve(valuationDate, creditCurve);
        validateDiscountCurve(valuationDate, discountCurve);
    }

    public CvaCreditMethod creditMethod() {
        return creditCurve.isEmpty() ? CvaCreditMethod.FLAT_HAZARD_RATE : CvaCreditMethod.CREDIT_CURVE;
    }

    public CvaDiscountMethod discountMethod() {
        return discountCurve.isEmpty() ? CvaDiscountMethod.FLAT_RATE : CvaDiscountMethod.DISCOUNT_CURVE;
    }

    private static void validateCreditCurve(LocalDate valuationDate, List<CreditCurvePoint> creditCurve) {
        LocalDate previousDate = null;
        Double previousSurvival = null;
        CreditCurvePoint previousPoint = null;
        for (CreditCurvePoint point : creditCurve.stream().sorted(java.util.Comparator.comparing(CreditCurvePoint::date)).toList()) {
            if (!point.date().isAfter(valuationDate)) {
                throw new IllegalArgumentException("creditCurve dates must be after valuationDate");
            }
            if (previousDate != null && point.date().isEqual(previousDate)) {
                throw new IllegalArgumentException("creditCurve dates must not contain duplicates");
            }
            double survival = point.resolvedSurvivalProbability();
            if (previousSurvival != null && survival > previousSurvival) {
                if (previousPoint.usesCumulativeDefaultProbability() && point.usesCumulativeDefaultProbability()) {
                    throw new IllegalArgumentException("creditCurve cumulativeDefaultProbability must not decrease over time");
                }
                throw new IllegalArgumentException("creditCurve survivalProbability must not increase over time");
            }
            previousDate = point.date();
            previousSurvival = survival;
            previousPoint = point;
        }
    }

    private static void validateDiscountCurve(LocalDate valuationDate, List<DiscountCurvePoint> discountCurve) {
        LocalDate previousDate = null;
        for (DiscountCurvePoint point : discountCurve.stream().sorted(java.util.Comparator.comparing(DiscountCurvePoint::date)).toList()) {
            if (!point.date().isAfter(valuationDate)) {
                throw new IllegalArgumentException("discountCurve dates must be after valuationDate");
            }
            if (previousDate != null && point.date().isEqual(previousDate)) {
                throw new IllegalArgumentException("discountCurve dates must not contain duplicates");
            }
            previousDate = point.date();
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
