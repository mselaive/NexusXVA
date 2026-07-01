package com.nexusxva.cva.application;

import com.nexusxva.cva.domain.CvaPoint;
import com.nexusxva.cva.domain.CvaCreditMethod;
import com.nexusxva.cva.domain.CvaDiscountMethod;
import com.nexusxva.exposure.application.ExposureSimulationResult;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CvaCalculationResult(
        UUID portfolioId,
        LocalDate valuationDate,
        String model,
        String exposureModel,
        String baseCurrency,
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

    public CvaCalculationResult {
        if (portfolioId == null) {
            throw new IllegalArgumentException("portfolioId is required");
        }
        if (valuationDate == null) {
            throw new IllegalArgumentException("valuationDate is required");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("cva model is required");
        }
        if (exposureModel == null || exposureModel.isBlank()) {
            throw new IllegalArgumentException("exposure model is required");
        }
        if (creditMethod == null) {
            throw new IllegalArgumentException("creditMethod is required");
        }
        if (discountMethod == null) {
            throw new IllegalArgumentException("discountMethod is required");
        }
        if (points == null) {
            throw new IllegalArgumentException("cva points are required");
        }
        points = List.copyOf(points);
    }

    public static CvaCalculationResult from(
            CvaCalculationCommand command,
            ExposureSimulationResult exposure,
            double cva,
            CvaCreditMethod creditMethod,
            CvaDiscountMethod discountMethod,
            List<CvaPoint> points
    ) {
        return new CvaCalculationResult(
                command.portfolioId(),
                command.valuationDate(),
                "SIMPLIFIED_CVA_V1",
                exposure.model(),
                exposure.baseCurrency(),
                command.paths(),
                command.timeSteps(),
                command.pfeConfidenceLevel(),
                command.lossGivenDefault(),
                command.counterpartyHazardRate(),
                command.discountRate(),
                creditMethod,
                discountMethod,
                cva,
                points
        );
    }
}
