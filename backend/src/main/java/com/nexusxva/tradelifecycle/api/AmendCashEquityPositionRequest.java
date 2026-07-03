package com.nexusxva.tradelifecycle.api;

import com.nexusxva.portfolio.application.AddCashEquityPositionCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record AmendCashEquityPositionRequest(
        @NotBlank @Size(max = 32) @Pattern(regexp = "[A-Za-z0-9._-]{1,32}") String underlyingSymbol,
        @NotNull BigDecimal quantity,
        BigDecimal executionPrice
) {

    AddCashEquityPositionCommand toCommand() {
        return new AddCashEquityPositionCommand(
                underlyingSymbol,
                quantity,
                executionPrice
        );
    }
}
