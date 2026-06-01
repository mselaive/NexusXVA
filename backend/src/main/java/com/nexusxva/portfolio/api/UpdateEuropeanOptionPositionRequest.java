package com.nexusxva.portfolio.api;

import com.nexusxva.instruments.domain.OptionType;
import com.nexusxva.portfolio.application.UpdateEuropeanOptionPositionCommand;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateEuropeanOptionPositionRequest(
        @Size(max = 32) @Pattern(regexp = "[A-Za-z0-9._-]{1,32}") String underlyingSymbol,
        OptionType optionType,
        @DecimalMin(value = "0.0", inclusive = false) BigDecimal strike,
        LocalDate maturityDate,
        BigDecimal quantity
) {

    UpdateEuropeanOptionPositionCommand toCommand() {
        return new UpdateEuropeanOptionPositionCommand(
                underlyingSymbol,
                optionType,
                strike,
                maturityDate,
                quantity
        );
    }
}
