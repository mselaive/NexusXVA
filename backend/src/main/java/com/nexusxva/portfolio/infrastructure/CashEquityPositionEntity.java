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
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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

    @OneToMany(mappedBy = "position", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("executedAt ASC")
    private List<CashEquityLotEntity> lots = new ArrayList<>();

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
        CashEquityPositionEntity entity = new CashEquityPositionEntity(position, portfolio);
        if (executionPrice != null) {
            entity.lots.add(CashEquityLotEntity.opening(entity, quantity, executionPrice, now));
        }
        return entity;
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
                updatedAt,
                lots.stream()
                        .map(CashEquityLotEntity::toDomain)
                        .toList()
        );
    }

    UUID getId() {
        return id;
    }

    void markCancelled() {
        lifecycleStatus = PositionLifecycleStatus.CANCELLED;
        updatedAt = Instant.now();
    }

    void markAmended() {
        lifecycleStatus = PositionLifecycleStatus.AMENDED;
        updatedAt = Instant.now();
    }

    void addAmendmentCloseLot(BigDecimal requestedQuantity, BigDecimal requestedExecutionPrice) {
        BigDecimal averageCost = toDomain().averageCost();
        if (averageCost == null || requestedExecutionPrice == null || requestedQuantity == null) {
            return;
        }
        BigDecimal closedQuantity = quantity.subtract(requestedQuantity);
        if (!isReduction(quantity, requestedQuantity, closedQuantity)) {
            return;
        }
        BigDecimal realizedPnl = closedQuantity.multiply(requestedExecutionPrice.subtract(averageCost));
        lots.add(CashEquityLotEntity.amendmentClose(
                this,
                closedQuantity,
                requestedExecutionPrice,
                averageCost,
                realizedPnl,
                Instant.now()
        ));
    }

    private boolean isReduction(BigDecimal originalQuantity, BigDecimal requestedQuantity, BigDecimal closedQuantity) {
        return originalQuantity.signum() == requestedQuantity.signum()
                && requestedQuantity.abs().compareTo(originalQuantity.abs()) < 0
                && closedQuantity.signum() == originalQuantity.signum();
    }
}
