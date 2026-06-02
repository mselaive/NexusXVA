package com.nexusxva.portfolio.api;

import com.nexusxva.instruments.domain.OptionType;
import com.nexusxva.portfolio.domain.EuropeanOptionPosition;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EuropeanOptionPositionResponse(
        UUID id,
        UUID portfolioId,
        String underlyingSymbol,
        OptionType optionType,
        BigDecimal strike,
        LocalDate maturityDate,
        BigDecimal quantity,
        Instant createdAt,
        Instant updatedAt
) {

    static EuropeanOptionPositionResponse from(EuropeanOptionPosition position) {
        return new EuropeanOptionPositionResponse(
                position.id(),
                position.portfolioId(),
                position.underlyingSymbol(),
                position.optionType(),
                position.strike(),
                position.maturityDate(),
                position.quantity(),
                position.createdAt(),
                position.updatedAt()
        );
    }
}
