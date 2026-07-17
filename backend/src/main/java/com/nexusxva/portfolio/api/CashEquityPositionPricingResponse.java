package com.nexusxva.portfolio.api;

import com.nexusxva.portfolio.application.CashEquityPositionPricingResult;
import java.util.UUID;

public record CashEquityPositionPricingResponse(
        UUID positionId,
        String status,
        String underlyingSymbol,
        double quantity,
        double spot,
        double marketValue,
        Double executionPrice,
        Double averageCost,
        Double costBasis,
        Double tradeValue,
        Double unrealizedPnl,
        double realizedPnl,
        PortfolioGreeksResponse positionGreeks,
        PortfolioPositionMarketDataResponse marketData
) {

    public static CashEquityPositionPricingResponse from(CashEquityPositionPricingResult result) {
        return new CashEquityPositionPricingResponse(
                result.positionId(),
                result.status().name(),
                result.underlyingSymbol(),
                result.quantity(),
                result.spot(),
                result.marketValue(),
                result.executionPrice(),
                result.averageCost(),
                result.costBasis(),
                result.tradeValue(),
                result.unrealizedPnl(),
                result.realizedPnl(),
                PortfolioGreeksResponse.from(result.positionGreeks()),
                PortfolioPositionMarketDataResponse.from(result.marketData())
        );
    }
}
