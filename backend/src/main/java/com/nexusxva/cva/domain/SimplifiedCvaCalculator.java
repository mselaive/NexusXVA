package com.nexusxva.cva.domain;

import com.nexusxva.exposure.domain.ExposurePoint;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SimplifiedCvaCalculator {

    public CvaResult calculate(CvaInput input) {
        List<ExposurePoint> exposurePoints = input.exposurePoints()
                .stream()
                .sorted(Comparator.comparing(ExposurePoint::date))
                .toList();

        List<CvaPoint> cvaPoints = new ArrayList<>();
        double cva = 0.0;
        double previousSurvivalProbability = 1.0;
        List<CreditCurvePoint> creditCurve = sortedCreditCurve(input.creditCurve());
        List<DiscountCurvePoint> discountCurve = sortedDiscountCurve(input.discountCurve());

        for (ExposurePoint exposurePoint : exposurePoints) {
            double timeYears = ChronoUnit.DAYS.between(input.valuationDate(), exposurePoint.date()) / 365.0;
            double discountFactor = discountFactor(input, discountCurve, exposurePoint, timeYears);
            double survivalProbability = survivalProbability(input, creditCurve, exposurePoint, timeYears);
            double defaultProbabilityIncrement = previousSurvivalProbability - survivalProbability;
            double discountedExpectedExposure = discountFactor * exposurePoint.expectedExposure();
            double cvaContribution = input.lossGivenDefault() * discountedExpectedExposure * defaultProbabilityIncrement;

            CvaPoint point = new CvaPoint(
                    exposurePoint.date(),
                    exposurePoint.expectedExposure(),
                    discountFactor,
                    survivalProbability,
                    defaultProbabilityIncrement,
                    discountedExpectedExposure,
                    cvaContribution
            );
            cvaPoints.add(point);
            cva += cvaContribution;
            previousSurvivalProbability = survivalProbability;
        }

        return new CvaResult(cva, input.creditMethod(), input.discountMethod(), cvaPoints);
    }

    private double survivalProbability(
            CvaInput input,
            List<CreditCurvePoint> creditCurve,
            ExposurePoint exposurePoint,
            double timeYears
    ) {
        if (creditCurve.isEmpty()) {
            return Math.exp(-input.counterpartyHazardRate() * timeYears);
        }
        return interpolateCreditCurve(creditCurve, input.valuationDate(), exposurePoint.date());
    }

    private double discountFactor(
            CvaInput input,
            List<DiscountCurvePoint> discountCurve,
            ExposurePoint exposurePoint,
            double timeYears
    ) {
        if (discountCurve.isEmpty()) {
            return Math.exp(-input.discountRate() * timeYears);
        }
        return interpolateDiscountCurve(discountCurve, input.valuationDate(), exposurePoint.date());
    }

    private double interpolateCreditCurve(
            List<CreditCurvePoint> curve,
            java.time.LocalDate valuationDate,
            java.time.LocalDate date
    ) {
        CreditCurvePoint first = curve.getFirst();
        if (date.isBefore(first.date())) {
            return interpolate(
                    valuationDate,
                    1.0,
                    first.date(),
                    first.resolvedSurvivalProbability(),
                    date
            );
        }
        if (curve.size() == 1) {
            if (first.date().isEqual(date)) {
                return first.resolvedSurvivalProbability();
            }
            throw new IllegalArgumentException("creditCurve does not cover exposure date");
        }
        int index = lowerBoundIndex(curve.stream().map(CreditCurvePoint::date).toList(), date);
        if (index < 0 || index >= curve.size()) {
            throw new IllegalArgumentException("creditCurve does not cover exposure date");
        }
        CreditCurvePoint exactOrRight = curve.get(index);
        if (exactOrRight.date().isEqual(date)) {
            return exactOrRight.resolvedSurvivalProbability();
        }
        CreditCurvePoint left = curve.get(index - 1);
        return interpolate(
                left.date(),
                left.resolvedSurvivalProbability(),
                exactOrRight.date(),
                exactOrRight.resolvedSurvivalProbability(),
                date
        );
    }

    private double interpolateDiscountCurve(
            List<DiscountCurvePoint> curve,
            java.time.LocalDate valuationDate,
            java.time.LocalDate date
    ) {
        DiscountCurvePoint first = curve.getFirst();
        if (date.isBefore(first.date())) {
            return interpolate(valuationDate, 1.0, first.date(), first.discountFactor(), date);
        }
        if (curve.size() == 1) {
            if (first.date().isEqual(date)) {
                return first.discountFactor();
            }
            throw new IllegalArgumentException("discountCurve does not cover exposure date");
        }
        int index = lowerBoundIndex(curve.stream().map(DiscountCurvePoint::date).toList(), date);
        if (index < 0 || index >= curve.size()) {
            throw new IllegalArgumentException("discountCurve does not cover exposure date");
        }
        DiscountCurvePoint exactOrRight = curve.get(index);
        if (exactOrRight.date().isEqual(date)) {
            return exactOrRight.discountFactor();
        }
        DiscountCurvePoint left = curve.get(index - 1);
        return interpolate(left.date(), left.discountFactor(), exactOrRight.date(), exactOrRight.discountFactor(), date);
    }

    private double interpolate(java.time.LocalDate leftDate, double leftValue, java.time.LocalDate rightDate, double rightValue, java.time.LocalDate date) {
        double totalDays = ChronoUnit.DAYS.between(leftDate, rightDate);
        double elapsedDays = ChronoUnit.DAYS.between(leftDate, date);
        return leftValue + (rightValue - leftValue) * elapsedDays / totalDays;
    }

    private int lowerBoundIndex(List<java.time.LocalDate> dates, java.time.LocalDate target) {
        for (int index = 0; index < dates.size(); index++) {
            if (!dates.get(index).isBefore(target)) {
                return index;
            }
        }
        return -1;
    }

    private List<CreditCurvePoint> sortedCreditCurve(List<CreditCurvePoint> creditCurve) {
        return creditCurve.stream().sorted(Comparator.comparing(CreditCurvePoint::date)).toList();
    }

    private List<DiscountCurvePoint> sortedDiscountCurve(List<DiscountCurvePoint> discountCurve) {
        return discountCurve.stream().sorted(Comparator.comparing(DiscountCurvePoint::date)).toList();
    }
}
