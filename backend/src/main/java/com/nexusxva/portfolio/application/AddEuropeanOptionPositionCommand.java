package com.nexusxva.portfolio.application;

import com.nexusxva.instruments.domain.OptionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

public record AddEuropeanOptionPositionCommand(
        String underlyingSymbol,
        OptionType optionType,
        BigDecimal strike,
        LocalDate maturityDate,
        BigDecimal quantity
) {

    public AddEuropeanOptionPositionCommand {
        if (underlyingSymbol == null || underlyingSymbol.isBlank()) {
            throw new IllegalArgumentException("underlyingSymbol is required");
        }
        underlyingSymbol = underlyingSymbol.trim().toUpperCase(Locale.ROOT);
        if (!underlyingSymbol.matches("[A-Z0-9._-]{1,32}")) {
            throw new IllegalArgumentException("underlyingSymbol must use 1-32 letters, numbers, dots, underscores, or hyphens");
        }
        if (optionType == null) {
            throw new IllegalArgumentException("optionType is required");
        }
        if (strike == null || strike.signum() <= 0) {
            throw new IllegalArgumentException("strike must be greater than zero");
        }
        if (maturityDate == null) {
            throw new IllegalArgumentException("maturityDate is required");
        }
        if (quantity == null || quantity.signum() == 0) {
            throw new IllegalArgumentException("quantity must be non-zero");
        }

    }
}
