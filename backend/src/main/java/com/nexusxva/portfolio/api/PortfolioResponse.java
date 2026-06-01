package com.nexusxva.portfolio.api;

import com.nexusxva.portfolio.domain.Portfolio;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PortfolioResponse(
        UUID id,
        String name,
        String description,
        String baseCurrency,
        Instant createdAt,
        Instant updatedAt,
        List<EuropeanOptionPositionResponse> positions
) {

    static PortfolioResponse from(Portfolio portfolio) {
        return new PortfolioResponse(
                portfolio.id(),
                portfolio.name(),
                portfolio.description(),
                portfolio.baseCurrency(),
                portfolio.createdAt(),
                portfolio.updatedAt(),
                portfolio.positions()
                        .stream()
                        .map(EuropeanOptionPositionResponse::from)
                        .toList()
        );
    }
}
