package com.nexusxva.tradebooking.domain;

import com.nexusxva.instruments.domain.OptionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TradeBookingRequest(
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
        List<TradeBookingLeg> legs,
        TradeBookingStatus status,
        BookingActor submittedBy,
        Instant submittedAt,
        BookingActor reviewedBy,
        Instant reviewedAt,
        String rejectionReason,
        UUID confirmedPositionId,
        List<UUID> confirmedPositionIds
) {
}
