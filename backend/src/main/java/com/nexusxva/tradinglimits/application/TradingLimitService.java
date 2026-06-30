package com.nexusxva.tradinglimits.application;

import com.nexusxva.shared.error.ConflictException;
import com.nexusxva.shared.error.ResourceNotFoundException;
import com.nexusxva.tradebooking.application.CreateEuropeanOptionBookingCommand;
import com.nexusxva.tradebooking.application.CreateCashEquityBookingCommand;
import com.nexusxva.tradebooking.domain.BookingActor;
import com.nexusxva.tradinglimits.domain.TradingLimitPolicy;
import com.nexusxva.tradinglimits.domain.TradingLimitSnapshot;
import com.nexusxva.tradinglimits.domain.TradingLimitUsage;
import com.nexusxva.tradinglimits.domain.TradingLimitWindows;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TradingLimitService {

    private final TradingLimitStore store;
    private final Clock clock;

    @Autowired
    public TradingLimitService(TradingLimitStore store) {
        this(store, Clock.systemUTC());
    }

    TradingLimitService(TradingLimitStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    @Transactional
    public void validateBooking(
            BookingActor actor,
            String portfolioCurrency,
            CreateEuropeanOptionBookingCommand command
    ) {
        validateBooking(actor, portfolioCurrency, 1, command.quantity().abs().multiply(command.strike()));
    }

    @Transactional
    public void validateStrategyBooking(
            BookingActor actor,
            String portfolioCurrency,
            List<CreateEuropeanOptionBookingCommand> legs
    ) {
        BigDecimal requestedNotional = legs.stream()
                .map(leg -> leg.quantity().abs().multiply(leg.strike()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        validateBooking(actor, portfolioCurrency, 1, requestedNotional);
    }

    @Transactional
    public void validateCashEquityBooking(
            BookingActor actor,
            String portfolioCurrency,
            CreateCashEquityBookingCommand command
    ) {
        validateBooking(actor, portfolioCurrency, 1, command.bookingNotional() == null ? BigDecimal.ZERO : command.bookingNotional());
    }

    private void validateBooking(
            BookingActor actor,
            String portfolioCurrency,
            int requestedTrades,
            BigDecimal requestedNotional
    ) {
        if (actor.userId() == null) {
            return;
        }
        TradingLimitPolicy policy = store.findPolicyForUpdate(actor.userId()).orElse(null);
        if (policy == null || !policy.active()) {
            return;
        }
        if (policy.hasNotionalLimit() && !"USD".equals(portfolioCurrency)) {
            throw new ConflictException("Active notional limits support USD portfolios only");
        }

        TradingLimitWindows windows = TradingLimitWindows.at(Instant.now(clock));
        TradingLimitUsage usage = store.usage(actor.userId(), windows);

        enforce(
                "TRADES_PER_HOUR",
                policy.maxTradesPerHour(),
                usage.tradesThisHour(),
                requestedTrades,
                windows.hourEndsAt()
        );
        enforce(
                "TRADES_PER_DAY",
                policy.maxTradesPerDay(),
                usage.tradesToday(),
                requestedTrades,
                windows.dayEndsAt()
        );
        enforce(
                "NOTIONAL_PER_HOUR",
                policy.maxNotionalPerHour(),
                usage.notionalThisHour(),
                requestedNotional,
                windows.hourEndsAt()
        );
        enforce(
                "NOTIONAL_PER_DAY",
                policy.maxNotionalPerDay(),
                usage.notionalToday(),
                requestedNotional,
                windows.dayEndsAt()
        );
    }

    @Transactional(readOnly = true)
    public TradingLimitSnapshot mine(BookingActor actor) {
        if (actor.userId() == null) {
            return unlimitedSystemSnapshot(actor);
        }
        return store.snapshot(actor.userId(), TradingLimitWindows.at(Instant.now(clock)));
    }

    @Transactional(readOnly = true)
    public TradingLimitSnapshot get(UUID userId) {
        ensureFoUser(userId);
        return store.snapshot(userId, TradingLimitWindows.at(Instant.now(clock)));
    }

    @Transactional(readOnly = true)
    public TradingLimitUserPage search(String query, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return store.searchFoUsers(
                query == null ? null : query.trim(),
                safePage,
                safeSize,
                TradingLimitWindows.at(Instant.now(clock))
        );
    }

    @Transactional
    public TradingLimitSnapshot update(
            UUID userId,
            UpdateTradingLimitCommand command,
            BookingActor updatedBy
    ) {
        ensureFoUser(userId);
        store.savePolicy(userId, command, updatedBy);
        return store.snapshot(userId, TradingLimitWindows.at(Instant.now(clock)));
    }

    private void ensureFoUser(UUID userId) {
        if (!store.isActiveFoUser(userId)) {
            throw new ResourceNotFoundException("FO user not found");
        }
    }

    private void enforce(String type, Integer maximum, long current, int requested, Instant periodEndsAt) {
        if (maximum != null && current + requested > maximum) {
            throw new TradingLimitExceededException(type, maximum, current, requested, periodEndsAt);
        }
    }

    private void enforce(
            String type,
            BigDecimal maximum,
            BigDecimal current,
            BigDecimal requested,
            Instant periodEndsAt
    ) {
        if (maximum != null && current.add(requested).compareTo(maximum) > 0) {
            throw new TradingLimitExceededException(type, maximum, current, requested, periodEndsAt);
        }
    }

    private TradingLimitSnapshot unlimitedSystemSnapshot(BookingActor actor) {
        TradingLimitWindows windows = TradingLimitWindows.at(Instant.now(clock));
        return new TradingLimitSnapshot(
                null,
                actor.username(),
                actor.displayName(),
                "UNLIMITED",
                null,
                new TradingLimitUsage(
                        0,
                        0,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        windows.hourEndsAt(),
                        windows.dayEndsAt()
                )
        );
    }
}
