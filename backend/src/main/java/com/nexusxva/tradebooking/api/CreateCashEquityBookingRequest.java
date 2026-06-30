package com.nexusxva.tradebooking.api;

import com.nexusxva.tradebooking.application.CreateCashEquityBookingCommand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;

public record CreateCashEquityBookingRequest(
        @NotBlank
        @Pattern(regexp = "[A-Za-z0-9._-]{1,32}", message = "underlyingSymbol must use 1-32 letters, numbers, dots, underscores, or hyphens")
        String underlyingSymbol,

        @NotNull
        BigDecimal quantity,

        @DecimalMin(value = "0.0", inclusive = true)
        BigDecimal executionPrice
) {

    public CreateCashEquityBookingCommand toCommand() {
        return new CreateCashEquityBookingCommand(
                underlyingSymbol,
                quantity,
                executionPrice
        );
    }
}
