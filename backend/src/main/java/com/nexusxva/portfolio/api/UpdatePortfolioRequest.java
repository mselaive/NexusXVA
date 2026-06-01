package com.nexusxva.portfolio.api;

import com.nexusxva.portfolio.application.UpdatePortfolioCommand;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdatePortfolioRequest(
        @Size(max = 120) String name,
        @Size(max = 500) String description,
        @Pattern(regexp = "[A-Za-z]{3}") String baseCurrency
) {

    UpdatePortfolioCommand toCommand() {
        return new UpdatePortfolioCommand(name, description, baseCurrency);
    }
}
