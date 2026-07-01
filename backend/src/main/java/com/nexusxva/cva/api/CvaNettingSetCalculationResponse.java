package com.nexusxva.cva.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nexusxva.cva.application.CvaNettingSetCalculationResult;
import com.nexusxva.cva.domain.CvaCreditMethod;
import com.nexusxva.cva.domain.CvaDiscountMethod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CvaNettingSetCalculationResponse(
        UUID nettingSetId,
        UUID counterpartyId,
        String counterpartyName,
        String nettingSetName,
        String baseCurrency,
        BigDecimal collateralAmount,
        String collateralCurrency,
        int portfolioCount,
        LocalDate valuationDate,
        String model,
        String exposureModel,
        boolean profileLevelNettingApproximation,
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

    static CvaNettingSetCalculationResponse from(CvaNettingSetCalculationResult result) {
        return new CvaNettingSetCalculationResponse(
                result.nettingSetId(),
                result.counterpartyId(),
                result.counterpartyName(),
                result.nettingSetName(),
                result.baseCurrency(),
                result.collateralAmount(),
                result.collateralCurrency(),
                result.portfolioCount(),
                result.valuationDate(),
                result.model(),
                result.exposureModel(),
                true,
                result.paths(),
                result.timeSteps(),
                result.pfeConfidenceLevel(),
                result.lossGivenDefault(),
                result.counterpartyHazardRate(),
                result.discountRate(),
                result.creditMethod(),
                result.discountMethod(),
                result.cva(),
                result.points().stream().map(CvaPointResponse::from).toList()
        );
    }
}
