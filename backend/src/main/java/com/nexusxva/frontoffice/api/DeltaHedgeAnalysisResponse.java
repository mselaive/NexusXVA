package com.nexusxva.frontoffice.api;

import com.nexusxva.frontoffice.application.DeltaHedgeAnalysisResult;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DeltaHedgeAnalysisResponse(
        UUID portfolioId,
        LocalDate valuationDate,
        String model,
        String baseCurrency,
        List<DeltaHedgeRowResponse> rows
) {
    static DeltaHedgeAnalysisResponse from(DeltaHedgeAnalysisResult result) {
        return new DeltaHedgeAnalysisResponse(
                result.portfolioId(),
                result.valuationDate(),
                result.model(),
                result.baseCurrency(),
                result.rows().stream().map(DeltaHedgeRowResponse::from).toList()
        );
    }
}
