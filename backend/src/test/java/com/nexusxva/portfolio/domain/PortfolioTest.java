package com.nexusxva.portfolio.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class PortfolioTest {

    @Test
    void trimsPortfolioName() {
        Instant now = Instant.now();
        Portfolio portfolio = new Portfolio(UUID.randomUUID(), "  Rates Book  ", null, null, now, now, List.of());

        assertThat(portfolio.name()).isEqualTo("Rates Book");
    }

    @Test
    void acceptsOptionalDescription() {
        Instant now = Instant.now();
        Portfolio portfolio = new Portfolio(UUID.randomUUID(), "Rates Book", "  Desk portfolio  ", "usd", now, now, List.of());

        assertThat(portfolio.description()).isEqualTo("Desk portfolio");
    }

    @Test
    void defaultsBaseCurrencyToUsd() {
        Instant now = Instant.now();
        Portfolio portfolio = new Portfolio(UUID.randomUUID(), "Rates Book", null, null, now, now, List.of());

        assertThat(portfolio.baseCurrency()).isEqualTo("USD");
    }

    @Test
    void rejectsInvalidBaseCurrency() {
        Instant now = Instant.now();

        assertThatThrownBy(() -> new Portfolio(UUID.randomUUID(), "Rates Book", null, "US1", now, now, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("baseCurrency must be a 3-letter currency code");
    }

    @Test
    void rejectsMissingUpdatedAt() {
        Instant now = Instant.now();

        assertThatThrownBy(() -> new Portfolio(UUID.randomUUID(), "Rates Book", null, "USD", now, null, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("portfolio updatedAt is required");
    }

    @Test
    void rejectsBlankName() {
        Instant now = Instant.now();

        assertThatThrownBy(() -> new Portfolio(UUID.randomUUID(), " ", null, "USD", now, now, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("portfolio name is required");
    }
}
