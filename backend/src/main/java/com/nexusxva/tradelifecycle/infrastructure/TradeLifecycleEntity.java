package com.nexusxva.tradelifecycle.infrastructure;

import com.nexusxva.instruments.domain.OptionType;
import com.nexusxva.portfolio.application.AddEuropeanOptionPositionCommand;
import com.nexusxva.portfolio.domain.EuropeanOptionPosition;
import com.nexusxva.tradebooking.domain.BookingActor;
import com.nexusxva.tradelifecycle.domain.TradeLifecycleRequest;
import com.nexusxva.tradelifecycle.domain.TradeLifecycleRequestStatus;
import com.nexusxva.tradelifecycle.domain.TradeLifecycleRequestType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "trade_lifecycle_requests")
class TradeLifecycleEntity {

    @Id
    private UUID id;

    @Column(name = "portfolio_id")
    private UUID portfolioId;

    @Column(name = "portfolio_name", nullable = false, length = 120)
    private String portfolioName;

    @Column(name = "position_id")
    private UUID positionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 16)
    private TradeLifecycleRequestType requestType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TradeLifecycleRequestStatus status;

    @Column(name = "original_underlying_symbol", nullable = false, length = 32)
    private String originalUnderlyingSymbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "original_option_type", nullable = false, length = 16)
    private OptionType originalOptionType;

    @Column(name = "original_strike", nullable = false, precision = 19, scale = 8)
    private BigDecimal originalStrike;

    @Column(name = "original_maturity_date", nullable = false)
    private LocalDate originalMaturityDate;

    @Column(name = "original_quantity", nullable = false, precision = 19, scale = 8)
    private BigDecimal originalQuantity;

    @Column(name = "requested_underlying_symbol", length = 32)
    private String requestedUnderlyingSymbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "requested_option_type", length = 16)
    private OptionType requestedOptionType;

    @Column(name = "requested_strike", precision = 19, scale = 8)
    private BigDecimal requestedStrike;

    @Column(name = "requested_maturity_date")
    private LocalDate requestedMaturityDate;

    @Column(name = "requested_quantity", precision = 19, scale = 8)
    private BigDecimal requestedQuantity;

    @Column(name = "submitted_by_user_id")
    private UUID submittedByUserId;

    @Column(name = "submitted_by_username", length = 120)
    private String submittedByUsername;

    @Column(name = "submitted_by_display_name", length = 160)
    private String submittedByDisplayName;

    @Column(name = "submitted_at", nullable = false)
    private Instant submittedAt;

    @Column(name = "reviewed_by_user_id")
    private UUID reviewedByUserId;

    @Column(name = "reviewed_by_username", length = 120)
    private String reviewedByUsername;

    @Column(name = "reviewed_by_display_name", length = 160)
    private String reviewedByDisplayName;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "resulting_position_id")
    private UUID resultingPositionId;

    @Version
    private long version;

    protected TradeLifecycleEntity() {
    }

    static TradeLifecycleEntity amend(
            EuropeanOptionPosition original,
            String portfolioName,
            AddEuropeanOptionPositionCommand requested,
            BookingActor submittedBy
    ) {
        TradeLifecycleEntity entity = base(original, portfolioName, TradeLifecycleRequestType.AMEND, submittedBy);
        entity.requestedUnderlyingSymbol = requested.underlyingSymbol();
        entity.requestedOptionType = requested.optionType();
        entity.requestedStrike = requested.strike();
        entity.requestedMaturityDate = requested.maturityDate();
        entity.requestedQuantity = requested.quantity();
        return entity;
    }

    static TradeLifecycleEntity cancel(
            EuropeanOptionPosition original,
            String portfolioName,
            BookingActor submittedBy
    ) {
        return base(original, portfolioName, TradeLifecycleRequestType.CANCEL, submittedBy);
    }

    private static TradeLifecycleEntity base(
            EuropeanOptionPosition original,
            String portfolioName,
            TradeLifecycleRequestType type,
            BookingActor submittedBy
    ) {
        TradeLifecycleEntity entity = new TradeLifecycleEntity();
        entity.id = UUID.randomUUID();
        entity.portfolioId = original.portfolioId();
        entity.portfolioName = portfolioName;
        entity.positionId = original.id();
        entity.requestType = type;
        entity.status = TradeLifecycleRequestStatus.PENDING_VALIDATION;
        entity.originalUnderlyingSymbol = original.underlyingSymbol();
        entity.originalOptionType = original.optionType();
        entity.originalStrike = original.strike();
        entity.originalMaturityDate = original.maturityDate();
        entity.originalQuantity = original.quantity();
        entity.submittedByUserId = submittedBy.userId();
        entity.submittedByUsername = submittedBy.username();
        entity.submittedByDisplayName = submittedBy.displayName();
        entity.submittedAt = Instant.now();
        return entity;
    }

    void approve(BookingActor reviewer, UUID resultingPositionId) {
        status = TradeLifecycleRequestStatus.APPROVED;
        reviewedByUserId = reviewer.userId();
        reviewedByUsername = reviewer.username();
        reviewedByDisplayName = reviewer.displayName();
        reviewedAt = Instant.now();
        rejectionReason = null;
        this.resultingPositionId = resultingPositionId;
    }

    void reject(BookingActor reviewer, String reason) {
        status = TradeLifecycleRequestStatus.REJECTED;
        reviewedByUserId = reviewer.userId();
        reviewedByUsername = reviewer.username();
        reviewedByDisplayName = reviewer.displayName();
        reviewedAt = Instant.now();
        rejectionReason = reason;
        resultingPositionId = null;
    }

    TradeLifecycleRequest toDomain() {
        return new TradeLifecycleRequest(
                id,
                portfolioId,
                portfolioName,
                positionId,
                requestType,
                status,
                originalUnderlyingSymbol,
                originalOptionType,
                originalStrike,
                originalMaturityDate,
                originalQuantity,
                requestedUnderlyingSymbol,
                requestedOptionType,
                requestedStrike,
                requestedMaturityDate,
                requestedQuantity,
                actor(submittedByUserId, submittedByUsername, submittedByDisplayName),
                submittedAt,
                actor(reviewedByUserId, reviewedByUsername, reviewedByDisplayName),
                reviewedAt,
                rejectionReason,
                resultingPositionId
        );
    }

    private BookingActor actor(UUID userId, String username, String displayName) {
        if (userId == null && username == null && displayName == null) {
            return null;
        }
        return new BookingActor(userId, username, displayName);
    }
}
