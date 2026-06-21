package com.nexusxva.tradelifecycle.domain;

import com.nexusxva.instruments.domain.OptionType;
import com.nexusxva.tradebooking.domain.BookingActor;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TradeLifecycleRequest(
        UUID id,
        UUID portfolioId,
        String portfolioName,
        UUID positionId,
        TradeLifecycleRequestType requestType,
        TradeLifecycleRequestStatus status,
        String originalUnderlyingSymbol,
        OptionType originalOptionType,
        BigDecimal originalStrike,
        LocalDate originalMaturityDate,
        BigDecimal originalQuantity,
        String requestedUnderlyingSymbol,
        OptionType requestedOptionType,
        BigDecimal requestedStrike,
        LocalDate requestedMaturityDate,
        BigDecimal requestedQuantity,
        BookingActor submittedBy,
        Instant submittedAt,
        BookingActor reviewedBy,
        Instant reviewedAt,
        String rejectionReason,
        UUID resultingPositionId
) {
}
