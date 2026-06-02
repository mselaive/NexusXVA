package com.nexusxva.portfolio.api;

import com.nexusxva.portfolio.application.UnpriceablePortfolioPosition;

import java.util.UUID;

public record UnpriceablePortfolioPositionResponse(
        UUID positionId,
        String status,
        String reason
) {

    public static UnpriceablePortfolioPositionResponse from(UnpriceablePortfolioPosition position) {
        return new UnpriceablePortfolioPositionResponse(
                position.positionId(),
                position.status().name(),
                position.reason()
        );
    }
}
