package com.nexusxva.tradebooking.domain;

import com.nexusxva.instruments.domain.OptionType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TradeBookingLeg(
        int legIndex,
        OptionType optionType,
        BigDecimal strike,
        LocalDate maturityDate,
        BigDecimal quantity,
        BigDecimal executionPrice
) {
}
