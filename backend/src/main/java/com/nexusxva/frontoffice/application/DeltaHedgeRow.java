package com.nexusxva.frontoffice.application;

public record DeltaHedgeRow(
        String symbol,
        double optionDeltaShares,
        double cashEquityDeltaShares,
        double netDeltaShares,
        double targetDeltaShares,
        double suggestedCashEquityQuantity,
        double spot,
        double estimatedTradeNotional
) {
}
