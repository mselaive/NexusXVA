package com.nexusxva.frontoffice.api;

import com.nexusxva.frontoffice.application.DeltaHedgeRow;

public record DeltaHedgeRowResponse(
        String symbol,
        double optionDeltaShares,
        double cashEquityDeltaShares,
        double netDeltaShares,
        double targetDeltaShares,
        double suggestedCashEquityQuantity,
        double spot,
        double estimatedTradeNotional
) {
    static DeltaHedgeRowResponse from(DeltaHedgeRow row) {
        return new DeltaHedgeRowResponse(
                row.symbol(),
                row.optionDeltaShares(),
                row.cashEquityDeltaShares(),
                row.netDeltaShares(),
                row.targetDeltaShares(),
                row.suggestedCashEquityQuantity(),
                row.spot(),
                row.estimatedTradeNotional()
        );
    }
}
