package com.nexusxva.xva.api;

import com.nexusxva.xva.application.CreateNettingSetCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateNettingSetRequest(
        @NotNull UUID counterpartyId,
        @NotBlank @Size(max = 160) String name,
        @Pattern(regexp = "[A-Za-z]{3}") String baseCurrency,
        @PositiveOrZero BigDecimal collateralAmount,
        @Pattern(regexp = "[A-Za-z]{3}") String collateralCurrency
) {

    CreateNettingSetCommand toCommand() {
        return new CreateNettingSetCommand(counterpartyId, name, baseCurrency, collateralAmount, collateralCurrency);
    }
}
