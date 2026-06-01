package com.nexusxva.portfolio.infrastructure;

import com.nexusxva.instruments.domain.OptionType;
import com.nexusxva.portfolio.domain.EuropeanOptionPosition;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "portfolio_european_option_positions")
class EuropeanOptionPositionEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private PortfolioEntity portfolio;

    @Column(name = "underlying_symbol", nullable = false, length = 32)
    private String underlyingSymbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "option_type", nullable = false, length = 16)
    private OptionType optionType;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal strike;

    @Column(name = "maturity_date", nullable = false)
    private LocalDate maturityDate;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal quantity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected EuropeanOptionPositionEntity() {
    }

    private EuropeanOptionPositionEntity(EuropeanOptionPosition position, PortfolioEntity portfolio) {
        this.id = position.id();
        this.portfolio = portfolio;
        this.underlyingSymbol = position.underlyingSymbol();
        this.optionType = position.optionType();
        this.strike = position.strike();
        this.maturityDate = position.maturityDate();
        this.quantity = position.quantity();
        this.createdAt = position.createdAt();
    }

    static EuropeanOptionPositionEntity create(
            PortfolioEntity portfolio,
            String underlyingSymbol,
            OptionType optionType,
            BigDecimal strike,
            LocalDate maturityDate,
            BigDecimal quantity
    ) {
        EuropeanOptionPosition position = new EuropeanOptionPosition(
                UUID.randomUUID(),
                portfolio.getId(),
                underlyingSymbol,
                optionType,
                strike,
                maturityDate,
                quantity,
                Instant.now()
        );

        return new EuropeanOptionPositionEntity(position, portfolio);
    }

    EuropeanOptionPosition toDomain() {
        return new EuropeanOptionPosition(
                id,
                portfolio.getId(),
                underlyingSymbol,
                optionType,
                strike,
                maturityDate,
                quantity,
                createdAt
        );
    }

    void update(
            String underlyingSymbol,
            OptionType optionType,
            BigDecimal strike,
            LocalDate maturityDate,
            BigDecimal quantity
    ) {
        if (underlyingSymbol != null) {
            this.underlyingSymbol = underlyingSymbol.trim().toUpperCase(Locale.ROOT);
        }
        if (optionType != null) {
            this.optionType = optionType;
        }
        if (strike != null) {
            this.strike = strike;
        }
        if (maturityDate != null) {
            this.maturityDate = maturityDate;
        }
        if (quantity != null) {
            this.quantity = quantity;
        }
    }
}
