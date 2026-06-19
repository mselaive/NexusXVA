package com.nexusxva.cva.api;

import com.nexusxva.cva.application.CvaCalculationCommand;
import com.nexusxva.cva.domain.CreditCurvePoint;
import com.nexusxva.cva.domain.DiscountCurvePoint;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CvaCalculationRequest(
        @NotNull UUID portfolioId,
        @NotNull LocalDate valuationDate,
        @NotNull @Min(1) @Max(3650) Integer horizonDays,
        @NotNull @Min(1) @Max(3650) Integer timeSteps,
        @NotNull @Min(1) @Max(100000) Integer paths,
        @NotNull Long seed,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) @DecimalMax(value = "1.0", inclusive = false)
        Double pfeConfidenceLevel,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0")
        Double lossGivenDefault,
        @DecimalMin("0.0")
        Double counterpartyHazardRate,
        Double discountRate,
        List<@Valid CreditCurvePointRequest> creditCurve,
        List<@Valid DiscountCurvePointRequest> discountCurve
) {

    CvaCalculationCommand toCommand() {
        return new CvaCalculationCommand(
                portfolioId,
                valuationDate,
                horizonDays,
                timeSteps,
                paths,
                seed,
                pfeConfidenceLevel,
                lossGivenDefault,
                counterpartyHazardRate,
                discountRate,
                toCreditCurve(),
                toDiscountCurve()
        );
    }

    private List<CreditCurvePoint> toCreditCurve() {
        if (creditCurve == null) {
            return List.of();
        }
        return creditCurve.stream()
                .map(CreditCurvePointRequest::toDomain)
                .toList();
    }

    private List<DiscountCurvePoint> toDiscountCurve() {
        if (discountCurve == null) {
            return List.of();
        }
        return discountCurve.stream()
                .map(DiscountCurvePointRequest::toDomain)
                .toList();
    }

    public record CreditCurvePointRequest(
            @NotNull LocalDate date,
            Double survivalProbability,
            Double cumulativeDefaultProbability
    ) {

        CreditCurvePoint toDomain() {
            return new CreditCurvePoint(date, survivalProbability, cumulativeDefaultProbability);
        }
    }

    public record DiscountCurvePointRequest(
            @NotNull LocalDate date,
            @NotNull @DecimalMin("0.0") @DecimalMax("1.0")
            Double discountFactor
    ) {

        DiscountCurvePoint toDomain() {
            return new DiscountCurvePoint(date, discountFactor);
        }
    }
}
