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
                PortfolioGreeksResponse.from(result.unitGreeks()),
                PortfolioGreeksResponse.from(result.positionGreeks()),
                PortfolioPositionMarketDataResponse.from(result.marketData())
        );
    }
}
