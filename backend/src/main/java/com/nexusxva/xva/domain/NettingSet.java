package com.nexusxva.xva.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record NettingSet(
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
        List<NettingSetPortfolio> portfolios
) {
    public NettingSet {
        portfolios = portfolios == null ? List.of() : List.copyOf(portfolios);
    }
}
