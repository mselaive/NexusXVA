package com.nexusxva.eod.api;

import com.nexusxva.eod.application.PortfolioDailyPnl;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PortfolioDailyPnlResponse(
        UUID portfolioId,
        LocalDate valuationDate,
        LocalDate previousEodDate,
        String baseCurrency,
        double currentMarketValue,
        double dailyPnl,
        int positionsWithoutReference,
        List<PositionDailyPnlResponse> positions
) {
    static PortfolioDailyPnlResponse from(PortfolioDailyPnl pnl) {
        return new PortfolioDailyPnlResponse(
                pnl.portfolioId(),
                pnl.valuationDate(),
                pnl.previousEodDate(),
                pnl.baseCurrency(),
                pnl.currentMarketValue(),
                pnl.dailyPnl(),
                pnl.positionsWithoutReference(),
                pnl.positions().stream().map(PositionDailyPnlResponse::from).toList()
        );
    }
}
