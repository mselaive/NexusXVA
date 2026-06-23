package com.nexusxva.eod.application;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PortfolioDailyPnl(
        UUID portfolioId,
        LocalDate valuationDate,
        LocalDate previousEodDate,
        String baseCurrency,
        double currentMarketValue,
        double dailyPnl,
        int positionsWithoutReference,
        List<PositionDailyPnl> positions
) {
    public PortfolioDailyPnl {
        positions = List.copyOf(positions);
    }
}
