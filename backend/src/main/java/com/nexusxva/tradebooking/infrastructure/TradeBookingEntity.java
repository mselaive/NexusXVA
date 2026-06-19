package com.nexusxva.tradebooking.infrastructure;

import com.nexusxva.instruments.domain.OptionType;
import com.nexusxva.tradebooking.application.CreateEuropeanOptionBookingCommand;
import com.nexusxva.tradebooking.domain.BookingActor;
import com.nexusxva.tradebooking.domain.TradeBookingRequest;
import com.nexusxva.tradebooking.domain.TradeBookingStatus;
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
@Table(name = "trade_booking_requests")
class TradeBookingEntity {

    @Id
    private UUID id;

    @Column(name = "portfolio_id")
    private UUID portfolioId;

    @Column(name = "portfolio_name", nullable = false, length = 120)
    private String portfolioName;

    @Column(name = "instrument_type", nullable = false, length = 32)
    private String instrumentType;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TradeBookingStatus status;

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

    @Column(name = "confirmed_position_id")
    private UUID confirmedPositionId;

    @Version
    private long version;

    protected TradeBookingEntity() {
    }

    static TradeBookingEntity create(
            UUID portfolioId,
            String portfolioName,
            CreateEuropeanOptionBookingCommand command,
            BookingActor submittedBy
    ) {
        TradeBookingEntity entity = new TradeBookingEntity();
        entity.id = UUID.randomUUID();
        entity.portfolioId = portfolioId;
        entity.portfolioName = portfolioName;
        entity.instrumentType = "EUROPEAN_OPTION";
        entity.underlyingSymbol = command.underlyingSymbol();
        entity.optionType = command.optionType();
        entity.strike = command.strike();
        entity.maturityDate = command.maturityDate();
        entity.quantity = command.quantity();
        entity.status = TradeBookingStatus.PENDING_VALIDATION;
        entity.submittedByUserId = submittedBy.userId();
        entity.submittedByUsername = submittedBy.username();
        entity.submittedByDisplayName = submittedBy.displayName();
        entity.submittedAt = Instant.now();
        return entity;
    }

    void confirm(BookingActor reviewer, UUID positionId) {
        status = TradeBookingStatus.CONFIRMED;
        reviewedByUserId = reviewer.userId();
        reviewedByUsername = reviewer.username();
        reviewedByDisplayName = reviewer.displayName();
        reviewedAt = Instant.now();
        confirmedPositionId = positionId;
        rejectionReason = null;
    }

    void reject(BookingActor reviewer, String reason) {
        status = TradeBookingStatus.REJECTED;
        reviewedByUserId = reviewer.userId();
        reviewedByUsername = reviewer.username();
        reviewedByDisplayName = reviewer.displayName();
        reviewedAt = Instant.now();
        rejectionReason = reason;
        confirmedPositionId = null;
    }

    TradeBookingRequest toDomain() {
        return new TradeBookingRequest(
                id,
                portfolioId,
                portfolioName,
                instrumentType,
                underlyingSymbol,
                optionType,
                strike,
                maturityDate,
                quantity,
                status,
                actor(submittedByUserId, submittedByUsername, submittedByDisplayName),
                submittedAt,
                actor(reviewedByUserId, reviewedByUsername, reviewedByDisplayName),
                reviewedAt,
                rejectionReason,
                confirmedPositionId
        );
    }

    private BookingActor actor(UUID userId, String username, String displayName) {
        if (userId == null && username == null && displayName == null) {
            return null;
        }
        return new BookingActor(userId, username, displayName);
    }
}

