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
        double sinceTradePnl,
        double optionDailyPnl,
        double cashEquityDailyPnl,
        double optionSinceTradePnl,
        double cashEquitySinceTradePnl,
        int positionsWithoutReference,
        int positionsWithoutExecutionPrice,
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
                pnl.sinceTradePnl(),
                pnl.optionDailyPnl(),
                pnl.cashEquityDailyPnl(),
                pnl.optionSinceTradePnl(),
                pnl.cashEquitySinceTradePnl(),
                pnl.positionsWithoutReference(),
                pnl.positionsWithoutExecutionPrice(),
                pnl.positions().stream().map(PositionDailyPnlResponse::from).toList()
        );
    }
}
