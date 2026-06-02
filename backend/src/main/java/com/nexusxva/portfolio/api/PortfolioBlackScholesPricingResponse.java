package com.nexusxva.portfolio.api;

import com.nexusxva.portfolio.application.PortfolioBlackScholesPricingResult;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PortfolioBlackScholesPricingResponse(
        UUID portfolioId,
        LocalDate valuationDate,
        String model,
        String baseCurrency,
        double totalPrice,
        PortfolioGreeksResponse totalGreeks,
        List<PortfolioPositionPricingResponse> positions,
        List<UnpriceablePortfolioPositionResponse> unpriceablePositions
) {

    public static PortfolioBlackScholesPricingResponse from(PortfolioBlackScholesPricingResult result) {
        return new PortfolioBlackScholesPricingResponse(
                result.portfolioId(),
                result.valuationDate(),
                result.model(),
                result.baseCurrency(),
                result.totalPrice(),
                PortfolioGreeksResponse.from(result.totalGreeks()),
                result.positions().stream()
                        .map(PortfolioPositionPricingResponse::from)
                        .toList(),
                result.unpriceablePositions().stream()
                        .map(UnpriceablePortfolioPositionResponse::from)
                        .toList()
        );
    }
}
