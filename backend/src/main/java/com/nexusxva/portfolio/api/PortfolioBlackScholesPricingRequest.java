package com.nexusxva.portfolio.api;

import java.time.LocalDate;

public record PortfolioBlackScholesPricingRequest(
        LocalDate valuationDate
) {
}
