package com.nexusxva.portfolio.infrastructure;

import com.nexusxva.portfolio.domain.CashEquityPosition;
import com.nexusxva.portfolio.domain.PositionLifecycleStatus;
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
import java.util.UUID;

@Entity
@Table(name = "portfolio_cash_equity_positions")
class CashEquityPositionEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private PortfolioEntity portfolio;

    @Column(name = "underlying_symbol", nullable = false, length = 32)
    private String underlyingSymbol;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal quantity;

    @Column(name = "execution_price", precision = 19, scale = 8)
    private BigDecimal executionPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_status", nullable = false, length = 16)
    private PositionLifecycleStatus lifecycleStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CashEquityPositionEntity() {
    }

    private CashEquityPositionEntity(CashEquityPosition position, PortfolioEntity portfolio) {
        this.id = position.id();
        this.portfolio = portfolio;
        this.underlyingSymbol = position.underlyingSymbol();
        this.quantity = position.quantity();
        this.executionPrice = position.executionPrice();
        this.lifecycleStatus = position.lifecycleStatus();
        this.createdAt = position.createdAt();
        this.updatedAt = position.updatedAt();
    }

    static CashEquityPositionEntity create(
            PortfolioEntity portfolio,
            String underlyingSymbol,
            BigDecimal quantity,
            BigDecimal executionPrice
    ) {
        Instant now = Instant.now();
        CashEquityPosition position = new CashEquityPosition(
                UUID.randomUUID(),
                portfolio.getId(),
                underlyingSymbol,
                quantity,
                executionPrice,
                PositionLifecycleStatus.ACTIVE,
                now,
                now
        );
        return new CashEquityPositionEntity(position, portfolio);
    }

    CashEquityPosition toDomain() {
        return new CashEquityPosition(
                id,
                portfolio.getId(),
                underlyingSymbol,
                quantity,
                executionPrice,
                lifecycleStatus,
                createdAt,
                updatedAt
        );
    }
}
