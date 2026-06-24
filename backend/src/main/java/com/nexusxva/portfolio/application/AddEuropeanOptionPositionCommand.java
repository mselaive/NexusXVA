package com.nexusxva.portfolio.application;

import com.nexusxva.instruments.domain.OptionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

public record AddEuropeanOptionPositionCommand(
        String underlyingSymbol,
        OptionType optionType,
        BigDecimal strike,
        LocalDate maturityDate,
        BigDecimal quantity,
        BigDecimal executionPrice,
        UUID strategyId,
        String strategyType,
        String strategyName,
        Integer strategyLegIndex
) {

    public AddEuropeanOptionPositionCommand(
            String underlyingSymbol,
            OptionType optionType,
            BigDecimal strike,
            LocalDate maturityDate,
            BigDecimal quantity
    ) {
        this(underlyingSymbol, optionType, strike, maturityDate, quantity, null);
    }

    public AddEuropeanOptionPositionCommand(
            String underlyingSymbol,
            OptionType optionType,
            BigDecimal strike,
            LocalDate maturityDate,
            BigDecimal quantity,
            BigDecimal executionPrice
    ) {
        this(underlyingSymbol, optionType, strike, maturityDate, quantity, executionPrice, null, null, null, null);
    }

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
        if (executionPrice != null && executionPrice.signum() < 0) {
            throw new IllegalArgumentException("executionPrice must be greater than or equal to zero");
        }
        if (strategyName != null) {
            strategyName = strategyName.trim();
            if (strategyName.isBlank()) {
                strategyName = null;
            } else if (strategyName.length() > 120) {
                throw new IllegalArgumentException("strategyName must be at most 120 characters");
            }
        }
        if (strategyType != null) {
            strategyType = strategyType.trim().toUpperCase(Locale.ROOT);
            if (strategyType.isBlank()) {
                strategyType = null;
            } else if (!strategyType.matches("[A-Z_]{1,32}")) {
                throw new IllegalArgumentException("strategyType must use 1-32 uppercase letters or underscores");
            }
        }
        if (strategyLegIndex != null && strategyLegIndex < 0) {
            throw new IllegalArgumentException("strategyLegIndex must be greater than or equal to zero");
        }

    }
}
