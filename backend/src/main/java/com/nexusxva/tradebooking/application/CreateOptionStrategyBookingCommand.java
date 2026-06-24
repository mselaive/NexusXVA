package com.nexusxva.tradebooking.application;

import com.nexusxva.tradebooking.domain.OptionStrategyType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public record CreateOptionStrategyBookingCommand(
        OptionStrategyType strategyType,
        String strategyName,
        String underlyingSymbol,
        List<CreateEuropeanOptionBookingCommand> legs
) {

    public CreateOptionStrategyBookingCommand {
        if (strategyType == null) {
            throw new IllegalArgumentException("strategyType is required");
        }
        if (underlyingSymbol == null || underlyingSymbol.isBlank()) {
            throw new IllegalArgumentException("underlyingSymbol is required");
        }
        String normalizedSymbol = underlyingSymbol.trim().toUpperCase(Locale.ROOT);
        if (!normalizedSymbol.matches("[A-Z0-9._-]{1,32}")) {
            throw new IllegalArgumentException("underlyingSymbol must use 1-32 letters, numbers, dots, underscores, or hyphens");
        }
        if (legs == null || legs.size() < 2) {
            throw new IllegalArgumentException("strategy requires at least two legs");
        }
        if (legs.size() > 6) {
            throw new IllegalArgumentException("strategy supports at most six legs");
        }
        underlyingSymbol = normalizedSymbol;
        String resolvedUnderlyingSymbol = underlyingSymbol;
        legs = legs.stream()
                .map(leg -> new CreateEuropeanOptionBookingCommand(
                        resolvedUnderlyingSymbol,
                        leg.optionType(),
                        leg.strike(),
                        leg.maturityDate(),
                        leg.quantity(),
                        leg.executionPrice()
                ))
                .toList();
        strategyName = strategyName == null || strategyName.isBlank()
                ? defaultName(strategyType, underlyingSymbol)
                : strategyName.trim();
        if (strategyName.length() > 120) {
            throw new IllegalArgumentException("strategyName must be at most 120 characters");
        }
    }

    public UUID newStrategyId() {
        return UUID.randomUUID();
    }

    public BigDecimal bookingNotional() {
        return legs.stream()
                .map(leg -> leg.quantity().abs().multiply(leg.strike()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String defaultName(OptionStrategyType strategyType, String underlyingSymbol) {
        return strategyType.name().replace('_', ' ') + " " + underlyingSymbol;
    }
}
