package com.nexusxva.tradebooking.api;

import com.nexusxva.instruments.domain.OptionType;
import com.nexusxva.tradebooking.domain.OptionStrategyType;
import com.nexusxva.tradebooking.domain.TradeBookingRequest;
import com.nexusxva.tradebooking.domain.TradeBookingStatus;
import com.nexusxva.tradebooking.domain.TradeBookingType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TradeBookingResponse(
        UUID id,
        UUID portfolioId,
        String portfolioName,
        String instrumentType,
        TradeBookingType bookingType,
        UUID strategyId,
        OptionStrategyType strategyType,
        String strategyName,
        String underlyingSymbol,
        OptionType optionType,
        BigDecimal strike,
        LocalDate maturityDate,
        BigDecimal quantity,
        BigDecimal executionPrice,
        BigDecimal bookingNotional,
        List<TradeBookingLegResponse> legs,
        TradeBookingStatus status,
        BookingActorResponse submittedBy,
        Instant submittedAt,
        BookingActorResponse reviewedBy,
        Instant reviewedAt,
        String rejectionReason,
        UUID confirmedPositionId,
        List<UUID> confirmedPositionIds
) {

    static TradeBookingResponse from(TradeBookingRequest booking) {
        return new TradeBookingResponse(
                booking.id(),
                booking.portfolioId(),
                booking.portfolioName(),
                booking.instrumentType(),
                booking.bookingType(),
                booking.strategyId(),
                booking.strategyType(),
                booking.strategyName(),
                booking.underlyingSymbol(),
                booking.optionType(),
                booking.strike(),
                booking.maturityDate(),
                booking.quantity(),
                booking.executionPrice(),
                booking.bookingNotional(),
                booking.legs().stream().map(TradeBookingLegResponse::from).toList(),
                booking.status(),
                BookingActorResponse.from(booking.submittedBy()),
                booking.submittedAt(),
                BookingActorResponse.from(booking.reviewedBy()),
                booking.reviewedAt(),
                booking.rejectionReason(),
                booking.confirmedPositionId(),
                booking.confirmedPositionIds()
        );
    }
}
