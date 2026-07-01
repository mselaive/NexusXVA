package com.nexusxva.xva.api;

import com.nexusxva.xva.domain.NettingSet;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record NettingSetResponse(
        UUID id,
        UUID counterpartyId,
        String counterpartyName,
        String name,
        String baseCurrency,
        BigDecimal collateralAmount,
        String collateralCurrency,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        List<NettingSetPortfolioResponse> portfolios
) {

    static NettingSetResponse from(NettingSet nettingSet) {
        return new NettingSetResponse(
                nettingSet.id(),
                nettingSet.counterpartyId(),
                nettingSet.counterpartyName(),
                nettingSet.name(),
                nettingSet.baseCurrency(),
                nettingSet.collateralAmount(),
                nettingSet.collateralCurrency(),
                nettingSet.active(),
                nettingSet.createdAt(),
                nettingSet.updatedAt(),
                nettingSet.portfolios().stream().map(NettingSetPortfolioResponse::from).toList()
        );
    }
}
