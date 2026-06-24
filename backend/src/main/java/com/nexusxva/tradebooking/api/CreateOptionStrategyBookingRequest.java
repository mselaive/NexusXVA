package com.nexusxva.tradebooking.api;

import com.nexusxva.instruments.domain.OptionType;
import com.nexusxva.tradebooking.application.CreateEuropeanOptionBookingCommand;
import com.nexusxva.tradebooking.application.CreateOptionStrategyBookingCommand;
import com.nexusxva.tradebooking.domain.OptionStrategyType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateOptionStrategyBookingRequest(
        @NotNull OptionStrategyType strategyType,
        @Size(max = 120) String strategyName,
        @NotBlank @Size(max = 32) @Pattern(regexp = "[A-Za-z0-9._-]{1,32}") String underlyingSymbol,
        @NotEmpty @Size(min = 2, max = 6) List<@Valid OptionStrategyLegRequest> legs
) {

    CreateOptionStrategyBookingCommand toCommand() {
        return new CreateOptionStrategyBookingCommand(
                strategyType,
                strategyName,
                underlyingSymbol,
                legs.stream().map(leg -> leg.toCommand(underlyingSymbol)).toList()
        );
    }

    public record OptionStrategyLegRequest(
            @NotNull OptionType optionType,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal strike,
            @NotNull LocalDate maturityDate,
            @NotNull BigDecimal quantity,
            @DecimalMin(value = "0.0") BigDecimal executionPrice
    ) {

        CreateEuropeanOptionBookingCommand toCommand(String underlyingSymbol) {
            return new CreateEuropeanOptionBookingCommand(
                    underlyingSymbol,
                    optionType,
                    strike,
                    maturityDate,
                    quantity,
                    executionPrice
            );
        }
    }
}
