package com.nexusxva.tradebooking.infrastructure;

import com.nexusxva.instruments.domain.OptionType;
import com.nexusxva.tradebooking.application.CreateCashEquityBookingCommand;
import com.nexusxva.tradebooking.application.CreateEuropeanOptionBookingCommand;
import com.nexusxva.tradebooking.application.CreateOptionStrategyBookingCommand;
import com.nexusxva.tradebooking.domain.BookingActor;
import com.nexusxva.tradebooking.domain.OptionStrategyType;
import com.nexusxva.tradebooking.domain.TradeBookingLeg;
import com.nexusxva.tradebooking.domain.TradeBookingRequest;
import com.nexusxva.tradebooking.domain.TradeBookingStatus;
import com.nexusxva.tradebooking.domain.TradeBookingType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "trade_booking_requests")
class TradeBookingEntity {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();
    private static final TypeReference<List<TradeBookingLeg>> LEGS_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<UUID>> IDS_TYPE = new TypeReference<>() {
    };

    @Id
    private UUID id;

    @Column(name = "portfolio_id")
    private UUID portfolioId;

    @Column(name = "portfolio_name", nullable = false, length = 120)
    private String portfolioName;

    @Column(name = "instrument_type", nullable = false, length = 32)
    private String instrumentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_type", nullable = false, length = 32)
    private TradeBookingType bookingType;

    @Column(name = "strategy_id")
    private UUID strategyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "strategy_type", length = 32)
    private OptionStrategyType strategyType;

    @Column(name = "strategy_name", length = 120)
    private String strategyName;

    @Column(name = "underlying_symbol", nullable = false, length = 32)
    private String underlyingSymbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "option_type", length = 16)
    private OptionType optionType;

    @Column(precision = 19, scale = 8)
    private BigDecimal strike;

    @Column(name = "maturity_date")
    private LocalDate maturityDate;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal quantity;

    @Column(name = "execution_price", precision = 19, scale = 8)
    private BigDecimal executionPrice;

    @Column(name = "booking_notional", precision = 19, scale = 8)
    private BigDecimal bookingNotional;

    @Column(name = "strategy_legs_json")
    private String strategyLegsJson;

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

    @Column(name = "confirmed_position_ids_json")
    private String confirmedPositionIdsJson;

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
        entity.bookingType = TradeBookingType.SINGLE_OPTION;
        entity.underlyingSymbol = command.underlyingSymbol();
        entity.optionType = command.optionType();
        entity.strike = command.strike();
        entity.maturityDate = command.maturityDate();
        entity.quantity = command.quantity();
        entity.executionPrice = command.executionPrice();
        entity.bookingNotional = command.quantity().abs().multiply(command.strike());
        entity.status = TradeBookingStatus.PENDING_VALIDATION;
        entity.submittedByUserId = submittedBy.userId();
        entity.submittedByUsername = submittedBy.username();
        entity.submittedByDisplayName = submittedBy.displayName();
        entity.submittedAt = Instant.now();
        return entity;
    }

    static TradeBookingEntity createCashEquity(
            UUID portfolioId,
            String portfolioName,
            CreateCashEquityBookingCommand command,
            BookingActor submittedBy
    ) {
        TradeBookingEntity entity = new TradeBookingEntity();
        entity.id = UUID.randomUUID();
        entity.portfolioId = portfolioId;
        entity.portfolioName = portfolioName;
        entity.instrumentType = "CASH_EQUITY";
        entity.bookingType = TradeBookingType.CASH_EQUITY;
        entity.underlyingSymbol = command.underlyingSymbol();
        entity.optionType = null;
        entity.strike = null;
        entity.maturityDate = null;
        entity.quantity = command.quantity();
        entity.executionPrice = command.executionPrice();
        entity.bookingNotional = command.bookingNotional();
        entity.status = TradeBookingStatus.PENDING_VALIDATION;
        entity.submittedByUserId = submittedBy.userId();
        entity.submittedByUsername = submittedBy.username();
        entity.submittedByDisplayName = submittedBy.displayName();
        entity.submittedAt = Instant.now();
        return entity;
    }

    static TradeBookingEntity createStrategy(
            UUID portfolioId,
            String portfolioName,
            CreateOptionStrategyBookingCommand command,
            UUID strategyId,
            BookingActor submittedBy
    ) {
        TradeBookingEntity entity = new TradeBookingEntity();
        CreateEuropeanOptionBookingCommand firstLeg = command.legs().getFirst();
        entity.id = UUID.randomUUID();
        entity.portfolioId = portfolioId;
        entity.portfolioName = portfolioName;
        entity.instrumentType = "EUROPEAN_OPTION";
        entity.bookingType = TradeBookingType.OPTION_STRATEGY;
        entity.strategyId = strategyId;
        entity.strategyType = command.strategyType();
        entity.strategyName = command.strategyName();
        entity.underlyingSymbol = command.underlyingSymbol();
        entity.optionType = firstLeg.optionType();
        entity.strike = firstLeg.strike();
        entity.maturityDate = firstLeg.maturityDate();
        entity.quantity = firstLeg.quantity();
        entity.executionPrice = null;
        entity.bookingNotional = command.bookingNotional();
        entity.strategyLegsJson = writeLegs(command);
        entity.status = TradeBookingStatus.PENDING_VALIDATION;
        entity.submittedByUserId = submittedBy.userId();
        entity.submittedByUsername = submittedBy.username();
        entity.submittedByDisplayName = submittedBy.displayName();
        entity.submittedAt = Instant.now();
        return entity;
    }

    void confirm(BookingActor reviewer, List<UUID> positionIds) {
        status = TradeBookingStatus.CONFIRMED;
        reviewedByUserId = reviewer.userId();
        reviewedByUsername = reviewer.username();
        reviewedByDisplayName = reviewer.displayName();
        reviewedAt = Instant.now();
        confirmedPositionId = positionIds == null || positionIds.isEmpty() ? null : positionIds.getFirst();
        confirmedPositionIdsJson = write(positionIds == null ? List.of() : positionIds);
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
        confirmedPositionIdsJson = null;
    }

    TradeBookingRequest toDomain() {
        return new TradeBookingRequest(
                id,
                portfolioId,
                portfolioName,
                instrumentType,
                bookingType == null ? TradeBookingType.SINGLE_OPTION : bookingType,
                strategyId,
                strategyType,
                strategyName,
                underlyingSymbol,
                optionType,
                strike,
                maturityDate,
                quantity,
                executionPrice,
                bookingNotional,
                read(strategyLegsJson, LEGS_TYPE),
                status,
                actor(submittedByUserId, submittedByUsername, submittedByDisplayName),
                submittedAt,
                actor(reviewedByUserId, reviewedByUsername, reviewedByDisplayName),
                reviewedAt,
                rejectionReason,
                confirmedPositionId,
                read(confirmedPositionIdsJson, IDS_TYPE)
        );
    }

    private static String writeLegs(CreateOptionStrategyBookingCommand command) {
        List<TradeBookingLeg> legs = command.legs().stream()
                .map(leg -> new TradeBookingLeg(
                        command.legs().indexOf(leg),
                        leg.optionType(),
                        leg.strike(),
                        leg.maturityDate(),
                        leg.quantity(),
                        leg.executionPrice()
                ))
                .toList();
        return write(legs);
    }

    private static String write(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Could not serialize trade booking strategy payload", exception);
        }
    }

    private static <T> T read(String json, TypeReference<T> type) {
        if (json == null || json.isBlank()) {
            if (type == LEGS_TYPE || type == IDS_TYPE) {
                return (T) List.of();
            }
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not deserialize trade booking strategy payload", exception);
        }
    }

    private BookingActor actor(UUID userId, String username, String displayName) {
        if (userId == null && username == null && displayName == null) {
            return null;
        }
        return new BookingActor(userId, username, displayName);
    }
}
