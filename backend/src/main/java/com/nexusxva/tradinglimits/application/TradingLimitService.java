package com.nexusxva.tradinglimits.application;

import com.nexusxva.marketdata.application.FxRateService;
import com.nexusxva.marketdata.domain.FxRate;
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
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TradingLimitService {

    private final TradingLimitStore store;
    private final FxRateService fxRateService;
    private final Clock clock;

    @Autowired
    public TradingLimitService(TradingLimitStore store, FxRateService fxRateService) {
        this(store, fxRateService, Clock.systemUTC());
    }

    TradingLimitService(TradingLimitStore store, FxRateService fxRateService, Clock clock) {
        this.store = store;
        this.fxRateService = fxRateService;
        this.clock = clock;
    }

    @Transactional
    public BigDecimal validateBooking(
            BookingActor actor,
            String portfolioCurrency,
            CreateEuropeanOptionBookingCommand command
    ) {
        return validateBooking(actor, portfolioCurrency, 1, command.quantity().abs().multiply(command.strike()));
    }

    @Transactional
    public BigDecimal validateStrategyBooking(
            BookingActor actor,
            String portfolioCurrency,
            List<CreateEuropeanOptionBookingCommand> legs
    ) {
        BigDecimal requestedNotional = legs.stream()
                .map(leg -> leg.quantity().abs().multiply(leg.strike()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return validateBooking(actor, portfolioCurrency, 1, requestedNotional);
    }

    @Transactional
    public BigDecimal validateCashEquityBooking(
            BookingActor actor,
            String portfolioCurrency,
            CreateCashEquityBookingCommand command
    ) {
        return validateBooking(actor, portfolioCurrency, 1, command.bookingNotional() == null ? BigDecimal.ZERO : command.bookingNotional());
    }

    private BigDecimal validateBooking(
            BookingActor actor,
            String portfolioCurrency,
            int requestedTrades,
            BigDecimal requestedNotional
    ) {
        BigDecimal requestedNotionalUsd = convertToPolicyCurrency(portfolioCurrency, requestedNotional);
        if (actor.userId() == null) {
            return requestedNotionalUsd;
        }
        TradingLimitPolicy policy = store.findPolicyForUpdate(actor.userId()).orElse(null);
        if (policy == null || !policy.active()) {
            return requestedNotionalUsd;
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
                requestedNotionalUsd,
                windows.hourEndsAt()
        );
        enforce(
                "NOTIONAL_PER_DAY",
                policy.maxNotionalPerDay(),
                usage.notionalToday(),
                requestedNotionalUsd,
                windows.dayEndsAt()
        );
        return requestedNotionalUsd;
    }

    private BigDecimal convertToPolicyCurrency(String portfolioCurrency, BigDecimal requestedNotional) {
        String sourceCurrency = portfolioCurrency == null || portfolioCurrency.isBlank()
                ? "USD"
                : portfolioCurrency.trim().toUpperCase(Locale.ROOT);
        if ("USD".equals(sourceCurrency)) {
            return requestedNotional;
        }
        FxRate rate = fxRateService.rate(sourceCurrency, "USD");
        return requestedNotional.multiply(BigDecimal.valueOf(rate.rate()));
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
