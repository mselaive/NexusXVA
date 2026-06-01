package com.nexusxva.portfolio.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexusxva.instruments.domain.OptionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class EuropeanOptionPositionTest {

    @Test
    void normalizesUnderlyingSymbol() {
        EuropeanOptionPosition position = validPosition("  aapl  ", new BigDecimal("10.0"));

        assertThat(position.underlyingSymbol()).isEqualTo("AAPL");
    }

    @Test
    void acceptsNegativeQuantityForShortPositions() {
        EuropeanOptionPosition position = validPosition("AAPL", new BigDecimal("-5.0"));

        assertThat(position.quantity()).isEqualByComparingTo("-5.0");
    }

    @Test
    void rejectsZeroQuantity() {
        assertThatThrownBy(() -> validPosition("AAPL", BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("quantity must be non-zero");
    }

    @Test
    void rejectsInvalidUnderlyingSymbol() {
        assertThatThrownBy(() -> validPosition("BAD SYMBOL", BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("underlyingSymbol must use 1-32 letters, numbers, dots, underscores, or hyphens");
    }

    @Test
    void rejectsNonPositiveStrike() {
        assertThatThrownBy(() -> new EuropeanOptionPosition(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "AAPL",
                OptionType.CALL,
                BigDecimal.ZERO,
                LocalDate.of(2027, 12, 31),
                BigDecimal.ONE,
                Instant.now()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("strike must be greater than zero");
    }

    private EuropeanOptionPosition validPosition(String symbol, BigDecimal quantity) {
        return new EuropeanOptionPosition(
                UUID.randomUUID(),
                UUID.randomUUID(),
                symbol,
                OptionType.CALL,
                new BigDecimal("100.0"),
                LocalDate.of(2027, 12, 31),
                quantity,
                Instant.now()
        );
    }
}
