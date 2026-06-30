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
        double totalTradeValue,
        double totalUnrealizedPnl,
        int positionsWithoutExecutionPrice,
        PortfolioGreeks totalGreeks,
        List<PortfolioPositionPricingResult> positions,
        List<CashEquityPositionPricingResult> cashEquityPositions,
        List<UnpriceablePortfolioPosition> unpriceablePositions
) {

    public PortfolioBlackScholesPricingResult {
        positions = List.copyOf(positions);
        cashEquityPositions = List.copyOf(cashEquityPositions);
        unpriceablePositions = List.copyOf(unpriceablePositions);
    }
}
