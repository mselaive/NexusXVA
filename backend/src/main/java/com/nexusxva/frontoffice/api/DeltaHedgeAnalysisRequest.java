package com.nexusxva.frontoffice.api;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record DeltaHedgeAnalysisRequest(
        @NotNull
        UUID portfolioId,
        LocalDate valuationDate,
        Map<String, Double> targetDeltaBySymbol
) {
}
