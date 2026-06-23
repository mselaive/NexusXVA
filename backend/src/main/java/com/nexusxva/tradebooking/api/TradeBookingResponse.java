package com.nexusxva.tradebooking.api;

import com.nexusxva.instruments.domain.OptionType;
import com.nexusxva.tradebooking.domain.TradeBookingRequest;
import com.nexusxva.tradebooking.domain.TradeBookingStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TradeBookingResponse(
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
        BookingActorResponse submittedBy,
        Instant submittedAt,
        BookingActorResponse reviewedBy,
        Instant reviewedAt,
        String rejectionReason,
        UUID confirmedPositionId
) {

    static TradeBookingResponse from(TradeBookingRequest booking) {
        return new TradeBookingResponse(
                booking.id(),
                booking.portfolioId(),
                booking.portfolioName(),
                booking.instrumentType(),
                booking.underlyingSymbol(),
                booking.optionType(),
                booking.strike(),
                booking.maturityDate(),
                booking.quantity(),
                booking.executionPrice(),
                booking.status(),
                BookingActorResponse.from(booking.submittedBy()),
                booking.submittedAt(),
                BookingActorResponse.from(booking.reviewedBy()),
                booking.reviewedAt(),
                booking.rejectionReason(),
                booking.confirmedPositionId()
        );
    }
}
