package com.nexusxva.tradebooking.application;

import com.nexusxva.portfolio.application.AddCashEquityPositionCommand;
import java.math.BigDecimal;

public record CreateCashEquityBookingCommand(
        String underlyingSymbol,
        BigDecimal quantity,
        BigDecimal executionPrice
) {

    public CreateCashEquityBookingCommand {
        AddCashEquityPositionCommand validated = new AddCashEquityPositionCommand(
                underlyingSymbol,
                quantity,
                executionPrice
        );
        underlyingSymbol = validated.underlyingSymbol();
    }

    public AddCashEquityPositionCommand toPositionCommand() {
        return new AddCashEquityPositionCommand(underlyingSymbol, quantity, executionPrice);
    }

    public BigDecimal bookingNotional() {
        return executionPrice == null ? null : quantity.abs().multiply(executionPrice);
    }
}
