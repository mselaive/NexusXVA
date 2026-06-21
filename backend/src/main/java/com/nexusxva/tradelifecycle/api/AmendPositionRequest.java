package com.nexusxva.tradelifecycle.api;

import com.nexusxva.instruments.domain.OptionType;
import com.nexusxva.portfolio.application.AddEuropeanOptionPositionCommand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record AmendPositionRequest(
        @NotBlank @Size(max = 32) @Pattern(regexp = "[A-Za-z0-9._-]{1,32}") String underlyingSymbol,
        @NotNull OptionType optionType,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal strike,
        @NotNull LocalDate maturityDate,
        @NotNull BigDecimal quantity
) {

    AddEuropeanOptionPositionCommand toCommand() {
        return new AddEuropeanOptionPositionCommand(
                underlyingSymbol,
                optionType,
                strike,
                maturityDate,
                quantity
        );
    }
}
