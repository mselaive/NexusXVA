package com.nexusxva.tradelifecycle.api;

import com.nexusxva.instruments.domain.OptionType;
import com.nexusxva.tradebooking.api.BookingActorResponse;
import com.nexusxva.tradelifecycle.domain.TradeLifecycleRequest;
import com.nexusxva.tradelifecycle.domain.TradeLifecycleRequestStatus;
import com.nexusxva.tradelifecycle.domain.TradeLifecycleRequestType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TradeLifecycleResponse(
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
        BookingActorResponse submittedBy,
        Instant submittedAt,
        BookingActorResponse reviewedBy,
        Instant reviewedAt,
        String rejectionReason,
        UUID resultingPositionId
) {

    static TradeLifecycleResponse from(TradeLifecycleRequest request) {
        return new TradeLifecycleResponse(
                request.id(),
                request.portfolioId(),
                request.portfolioName(),
                request.positionId(),
                request.requestType(),
                request.status(),
                request.originalUnderlyingSymbol(),
                request.originalOptionType(),
                request.originalStrike(),
                request.originalMaturityDate(),
                request.originalQuantity(),
                request.requestedUnderlyingSymbol(),
                request.requestedOptionType(),
                request.requestedStrike(),
                request.requestedMaturityDate(),
                request.requestedQuantity(),
                BookingActorResponse.from(request.submittedBy()),
                request.submittedAt(),
                BookingActorResponse.from(request.reviewedBy()),
                request.reviewedAt(),
                request.rejectionReason(),
                request.resultingPositionId()
        );
    }
}
