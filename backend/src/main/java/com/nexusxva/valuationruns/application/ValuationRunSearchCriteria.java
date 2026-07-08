package com.nexusxva.valuationruns.application;

import com.nexusxva.valuationruns.domain.ValuationRunStatus;
import com.nexusxva.valuationruns.domain.ValuationRunType;
import com.nexusxva.valuationruns.domain.ValuationRunScopeType;

import java.util.List;
import java.util.UUID;

public record ValuationRunSearchCriteria(
        ValuationRunType runType,
        ValuationRunStatus status,
        ValuationRunScopeType scopeType,
        UUID scopeId,
        UUID portfolioId,
        List<UUID> visiblePortfolioIds,
        List<UUID> visibleNettingSetIds,
        int limit
) {
    public ValuationRunSearchCriteria {
        limit = Math.max(1, Math.min(limit, 200));
        visiblePortfolioIds = visiblePortfolioIds == null ? null : List.copyOf(visiblePortfolioIds);
        visibleNettingSetIds = visibleNettingSetIds == null ? null : List.copyOf(visibleNettingSetIds);
    }
}
