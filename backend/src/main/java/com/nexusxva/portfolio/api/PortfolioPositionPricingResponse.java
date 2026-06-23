package com.nexusxva.portfolio.api;

import com.nexusxva.portfolio.application.PortfolioPositionPricingResult;

import java.util.UUID;

public record PortfolioPositionPricingResponse(
        UUID positionId,
        String status,
        String underlyingSymbol,
        double quantity,
        double unitPrice,
        double positionPrice,
        Double executionPrice,
        Double tradeValue,
        Double unrealizedPnl,
        PortfolioGreeksResponse unitGreeks,
        PortfolioGreeksResponse positionGreeks,
        PortfolioPositionMarketDataResponse marketData
) {

    public static PortfolioPositionPricingResponse from(PortfolioPositionPricingResult result) {
        return new PortfolioPositionPricingResponse(
                result.positionId(),
                result.status().name(),
                result.underlyingSymbol(),
                result.quantity(),
                result.unitPrice(),
                result.positionPrice(),
                result.executionPrice(),
                result.tradeValue(),
                result.unrealizedPnl(),
                PortfolioGreeksResponse.from(result.unitGreeks()),
                PortfolioGreeksResponse.from(result.positionGreeks()),
                PortfolioPositionMarketDataResponse.from(result.marketData())
        );
    }
}
