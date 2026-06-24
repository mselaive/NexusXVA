package com.nexusxva.tradebooking.api;

import com.nexusxva.instruments.domain.OptionType;
import com.nexusxva.tradebooking.domain.TradeBookingLeg;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TradeBookingLegResponse(
        int legIndex,
        OptionType optionType,
        BigDecimal strike,
        LocalDate maturityDate,
        BigDecimal quantity,
        BigDecimal executionPrice
) {

    static TradeBookingLegResponse from(TradeBookingLeg leg) {
        return new TradeBookingLegResponse(
                leg.legIndex(),
                leg.optionType(),
                leg.strike(),
                leg.maturityDate(),
                leg.quantity(),
                leg.executionPrice()
        );
    }
}
