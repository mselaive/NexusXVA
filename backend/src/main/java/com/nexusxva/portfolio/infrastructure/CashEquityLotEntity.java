package com.nexusxva.portfolio.infrastructure;

import com.nexusxva.portfolio.domain.CashEquityLot;
import com.nexusxva.portfolio.domain.CashEquityLotType;
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
@Table(name = "cash_equity_lots")
class CashEquityLotEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "position_id", nullable = false)
    private CashEquityPositionEntity position;

    @Enumerated(EnumType.STRING)
    @Column(name = "lot_type", nullable = false, length = 32)
    private CashEquityLotType lotType;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal quantity;

    @Column(name = "execution_price", precision = 19, scale = 8)
    private BigDecimal executionPrice;

    @Column(name = "average_cost", precision = 19, scale = 8)
    private BigDecimal averageCost;

    @Column(name = "realized_pnl", nullable = false, precision = 24, scale = 8)
    private BigDecimal realizedPnl;

    @Column(name = "executed_at", nullable = false)
    private Instant executedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CashEquityLotEntity() {
    }

    private CashEquityLotEntity(
            CashEquityPositionEntity position,
            CashEquityLotType lotType,
            BigDecimal quantity,
            BigDecimal executionPrice,
            BigDecimal averageCost,
            BigDecimal realizedPnl,
            Instant executedAt
    ) {
        this.id = UUID.randomUUID();
        this.position = position;
        this.lotType = lotType;
        this.quantity = quantity;
        this.executionPrice = executionPrice;
        this.averageCost = averageCost;
        this.realizedPnl = realizedPnl == null ? BigDecimal.ZERO : realizedPnl;
        this.executedAt = executedAt;
        this.createdAt = Instant.now();
    }

    static CashEquityLotEntity opening(CashEquityPositionEntity position, BigDecimal quantity, BigDecimal executionPrice, Instant executedAt) {
        return new CashEquityLotEntity(
                position,
                CashEquityLotType.OPENING,
                quantity,
                executionPrice,
                executionPrice,
                BigDecimal.ZERO,
                executedAt
        );
    }

    static CashEquityLotEntity amendmentClose(
            CashEquityPositionEntity position,
            BigDecimal closedQuantity,
            BigDecimal executionPrice,
            BigDecimal averageCost,
            BigDecimal realizedPnl,
            Instant executedAt
    ) {
        return new CashEquityLotEntity(
                position,
                CashEquityLotType.AMENDMENT_CLOSE,
                closedQuantity,
                executionPrice,
                averageCost,
                realizedPnl,
                executedAt
        );
    }

    CashEquityLot toDomain() {
        return new CashEquityLot(
                id,
                position.getId(),
                lotType,
                quantity,
                executionPrice,
                averageCost,
                realizedPnl,
                executedAt,
                createdAt
        );
    }
}
