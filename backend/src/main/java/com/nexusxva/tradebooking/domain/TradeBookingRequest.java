package com.nexusxva.tradebooking.domain;

import com.nexusxva.instruments.domain.OptionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TradeBookingRequest(
        UUID id,
        UUID portfolioId,
        String portfolioName,
        String instrumentType,
        String underlyingSymbol,
        OptionType optionType,
        BigDecimal strike,
        LocalDate maturityDate,
        BigDecimal quantity,
        BigDecimal executionPrice,
        TradeBookingStatus status,
        BookingActor submittedBy,
        Instant submittedAt,
        BookingActor reviewedBy,
        Instant reviewedAt,
        String rejectionReason,
        UUID confirmedPositionId
) {
}
