package com.nexusxva.portfolio.application;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PortfolioBlackScholesPricingResult(
        UUID portfolioId,
        LocalDate valuationDate,
        String model,
        String baseCurrency,
        double totalPrice,
        PortfolioGreeks totalGreeks,
        List<PortfolioPositionPricingResult> positions,
        List<UnpriceablePortfolioPosition> unpriceablePositions
) {

    public PortfolioBlackScholesPricingResult {
        positions = List.copyOf(positions);
        unpriceablePositions = List.copyOf(unpriceablePositions);
    }
}
