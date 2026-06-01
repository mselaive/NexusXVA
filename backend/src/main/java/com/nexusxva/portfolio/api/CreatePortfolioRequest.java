package com.nexusxva.portfolio.api;

import com.nexusxva.portfolio.application.CreatePortfolioCommand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreatePortfolioRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        @Pattern(regexp = "[A-Za-z]{3}") String baseCurrency
) {

    CreatePortfolioCommand toCommand() {
        return new CreatePortfolioCommand(name, description, baseCurrency);
    }
}
