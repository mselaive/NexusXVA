package com.nexusxva.cva.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nexusxva.cva.application.CvaCalculationResult;
import com.nexusxva.cva.domain.CvaCreditMethod;
import com.nexusxva.cva.domain.CvaDiscountMethod;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CvaCalculationResponse(
        UUID portfolioId,
        LocalDate valuationDate,
        String model,
        String exposureModel,
        int paths,
        int timeSteps,
        double pfeConfidenceLevel,
        double lossGivenDefault,
        Double counterpartyHazardRate,
        Double discountRate,
        CvaCreditMethod creditMethod,
        CvaDiscountMethod discountMethod,
        double cva,
        List<CvaPointResponse> points
) {

    static CvaCalculationResponse from(CvaCalculationResult result) {
        return new CvaCalculationResponse(
                result.portfolioId(),
                result.valuationDate(),
                result.model(),
                result.exposureModel(),
                result.paths(),
                result.timeSteps(),
                result.pfeConfidenceLevel(),
                result.lossGivenDefault(),
                result.counterpartyHazardRate(),
                result.discountRate(),
                result.creditMethod(),
                result.discountMethod(),
                result.cva(),
                result.points()
                        .stream()
                        .map(CvaPointResponse::from)
                        .toList()
        );
    }
}
