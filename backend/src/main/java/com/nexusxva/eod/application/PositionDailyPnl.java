package com.nexusxva.eod.application;

import java.util.UUID;

public record PositionDailyPnl(
        UUID positionId,
        String underlyingSymbol,
        double currentMarketValue,
        Double referenceValue,
        Double dailyPnl,
        String referenceMethod
) {
}
