package com.nexusxva.frontoffice.application;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DeltaHedgeAnalysisResult(
        UUID portfolioId,
        LocalDate valuationDate,
        String model,
        String baseCurrency,
        List<DeltaHedgeRow> rows
) {
    public DeltaHedgeAnalysisResult {
        rows = List.copyOf(rows);
    }
}
