package com.nexusxva.eod.api;

import com.nexusxva.eod.application.PositionDailyPnl;
import java.util.UUID;

public record PositionDailyPnlResponse(
        UUID positionId,
        String instrumentType,
        String underlyingSymbol,
        double currentMarketValue,
        Double referenceValue,
        Double dailyPnl,
        Double tradeValue,
        Double sinceTradePnl,
        String referenceMethod
) {
    static PositionDailyPnlResponse from(PositionDailyPnl pnl) {
        return new PositionDailyPnlResponse(
                pnl.positionId(),
                pnl.instrumentType(),
                pnl.underlyingSymbol(),
                pnl.currentMarketValue(),
                pnl.referenceValue(),
                pnl.dailyPnl(),
                pnl.tradeValue(),
                pnl.sinceTradePnl(),
                pnl.referenceMethod()
        );
    }
}
