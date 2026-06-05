package com.nexusxva.cva.api;

import com.nexusxva.cva.application.CvaCalculationResult;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CvaCalculationResponse(
        UUID portfolioId,
        LocalDate valuationDate,
        String model,
        String exposureModel,
        int paths,
        int timeSteps,
        double pfeConfidenceLevel,
        double lossGivenDefault,
        double counterpartyHazardRate,
        double discountRate,
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
                result.cva(),
                result.points()
                        .stream()
                        .map(CvaPointResponse::from)
                        .toList()
        );
    }
}
