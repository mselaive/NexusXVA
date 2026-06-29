package com.nexusxva.valuationruns.application;

import com.nexusxva.valuationruns.domain.ValuationRunStatus;
import com.nexusxva.valuationruns.domain.ValuationRunType;

import java.util.List;
import java.util.UUID;

public record ValuationRunSearchCriteria(
        ValuationRunType runType,
        ValuationRunStatus status,
        UUID portfolioId,
        List<UUID> visiblePortfolioIds,
        int limit
) {
    public ValuationRunSearchCriteria {
        limit = Math.max(1, Math.min(limit, 200));
        visiblePortfolioIds = visiblePortfolioIds == null ? null : List.copyOf(visiblePortfolioIds);
    }
}
