package com.nexusxva.cva.application;

import com.nexusxva.cva.domain.CvaCreditMethod;
import com.nexusxva.cva.domain.CvaDiscountMethod;
import com.nexusxva.cva.domain.CvaPoint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CvaNettingSetCalculationResult(
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
        int paths,
        int timeSteps,
        double pfeConfidenceLevel,
        double lossGivenDefault,
        Double counterpartyHazardRate,
        Double discountRate,
        CvaCreditMethod creditMethod,
        CvaDiscountMethod discountMethod,
        double cva,
        List<CvaPoint> points
) {

    public CvaNettingSetCalculationResult {
        points = points == null ? List.of() : List.copyOf(points);
    }
}
