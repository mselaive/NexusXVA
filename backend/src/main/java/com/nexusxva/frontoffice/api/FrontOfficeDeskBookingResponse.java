package com.nexusxva.frontoffice.api;

import com.nexusxva.instruments.domain.OptionType;
import com.nexusxva.frontoffice.application.FrontOfficeDeskService.FrontOfficeDeskBooking;
import com.nexusxva.tradebooking.api.BookingActorResponse;
import com.nexusxva.tradebooking.domain.TradeBookingRequest;
import com.nexusxva.tradebooking.domain.TradeBookingStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FrontOfficeDeskBookingResponse(
        UUID id,
        UUID portfolioId,
        String portfolioName,
        boolean portfolioVisible,
        String instrumentType,
        String underlyingSymbol,
        OptionType optionType,
        BigDecimal strike,
        LocalDate maturityDate,
        BigDecimal quantity,
        TradeBookingStatus status,
        BookingActorResponse submittedBy,
        Instant submittedAt,
        BookingActorResponse reviewedBy,
        Instant reviewedAt,
        String rejectionReason,
        UUID confirmedPositionId
) {

    static FrontOfficeDeskBookingResponse from(FrontOfficeDeskBooking deskBooking) {
        TradeBookingRequest booking = deskBooking.booking();
        return new FrontOfficeDeskBookingResponse(
                booking.id(),
                booking.portfolioId(),
                booking.portfolioName(),
                deskBooking.portfolioVisible(),
                booking.instrumentType(),
                booking.underlyingSymbol(),
                booking.optionType(),
                booking.strike(),
                booking.maturityDate(),
                booking.quantity(),
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
