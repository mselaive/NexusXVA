package com.nexusxva.eod.application;

import java.util.UUID;

public record PositionDailyPnl(
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
}
