package com.nexusxva.tradebooking.application;

import com.nexusxva.instruments.domain.OptionType;
import com.nexusxva.portfolio.application.AddEuropeanOptionPositionCommand;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateEuropeanOptionBookingCommand(
        String underlyingSymbol,
        OptionType optionType,
        BigDecimal strike,
        LocalDate maturityDate,
        BigDecimal quantity,
        BigDecimal executionPrice
) {

    public CreateEuropeanOptionBookingCommand(
            String underlyingSymbol,
            OptionType optionType,
            BigDecimal strike,
            LocalDate maturityDate,
            BigDecimal quantity
    ) {
        this(underlyingSymbol, optionType, strike, maturityDate, quantity, null);
    }

    public CreateEuropeanOptionBookingCommand {
        AddEuropeanOptionPositionCommand validated = new AddEuropeanOptionPositionCommand(
                underlyingSymbol,
                optionType,
                strike,
                maturityDate,
                quantity,
                executionPrice
        );
        underlyingSymbol = validated.underlyingSymbol();
    }

    public AddEuropeanOptionPositionCommand toPositionCommand() {
        return new AddEuropeanOptionPositionCommand(
                underlyingSymbol,
                optionType,
                strike,
                maturityDate,
                quantity,
                executionPrice
        );
    }
}
