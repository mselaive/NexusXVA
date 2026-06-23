package com.nexusxva.eod.api;

import com.nexusxva.eod.application.PositionDailyPnl;
import java.util.UUID;

public record PositionDailyPnlResponse(
        UUID positionId,
        String underlyingSymbol,
        double currentMarketValue,
        Double referenceValue,
        Double dailyPnl,
        String referenceMethod
) {
    static PositionDailyPnlResponse from(PositionDailyPnl pnl) {
        return new PositionDailyPnlResponse(
                pnl.positionId(),
                pnl.underlyingSymbol(),
                pnl.currentMarketValue(),
                pnl.referenceValue(),
                pnl.dailyPnl(),
                pnl.referenceMethod()
        );
    }
}
