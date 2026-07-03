package com.nexusxva.tradinglimits.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexusxva.instruments.domain.OptionType;
import com.nexusxva.marketdata.application.FxRateService;
import com.nexusxva.marketdata.domain.FxRate;
import com.nexusxva.tradebooking.application.CreateEuropeanOptionBookingCommand;
import com.nexusxva.tradebooking.domain.BookingActor;
import com.nexusxva.tradinglimits.domain.TradingLimitPolicy;
import com.nexusxva.tradinglimits.domain.TradingLimitSnapshot;
import com.nexusxva.tradinglimits.domain.TradingLimitUsage;
import com.nexusxva.tradinglimits.domain.TradingLimitWindows;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TradingLimitServiceTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final BookingActor ACTOR = new BookingActor(USER_ID, "raul", "Raul");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-20T10:15:00Z"), ZoneOffset.UTC);

    @Test
    void validatesAndReturnsUsdEquivalentNotionalForNonUsdPortfolio() {
        FakeStore store = new FakeStore(policy("1000"), usage("0"));
        TradingLimitService service = new TradingLimitService(store, fxRateService(), CLOCK);

        BigDecimal notional = service.validateBooking(ACTOR, "EUR", option("100", "1"));

        assertThat(notional).isEqualByComparingTo("109.0");
    }

    @Test
    void breachesLimitUsingConvertedUsdEquivalentNotional() {
        FakeStore store = new FakeStore(policy("105"), usage("0"));
        TradingLimitService service = new TradingLimitService(store, fxRateService(), CLOCK);

        assertThatThrownBy(() -> service.validateBooking(ACTOR, "EUR", option("100", "1")))
                .isInstanceOf(TradingLimitExceededException.class)
                .hasMessageContaining("NOTIONAL_PER_HOUR");
    }

    private static CreateEuropeanOptionBookingCommand option(String strike, String quantity) {
        return new CreateEuropeanOptionBookingCommand(
                "AAPL",
                OptionType.CALL,
                new BigDecimal(strike),
                LocalDate.of(2027, 6, 1),
                new BigDecimal(quantity)
        );
    }

    private static TradingLimitPolicy policy(String maxNotionalPerHour) {
        Instant now = Instant.parse("2026-06-20T10:00:00Z");
        return new TradingLimitPolicy(
                USER_ID,
                null,
                null,
                new BigDecimal(maxNotionalPerHour),
                new BigDecimal("10000"),
                "USD",
                true,
                now,
                now,
                USER_ID,
                "bo",
                "Back Office",
                0
        );
    }

    private static TradingLimitUsage usage(String notionalThisHour) {
        TradingLimitWindows windows = TradingLimitWindows.at(Instant.now(CLOCK));
        return new TradingLimitUsage(
                0,
                0,
                new BigDecimal(notionalThisHour),
                BigDecimal.ZERO,
                windows.hourEndsAt(),
                windows.dayEndsAt()
        );
    }

    private static FxRateService fxRateService() {
        return new FxRateService((sourceCurrency, targetCurrency) -> Optional.of(
                new FxRate(sourceCurrency, targetCurrency, 1.09, Instant.parse("2026-06-20T10:00:00Z"), "TEST", false)
        ));
    }

    private record FakeStore(TradingLimitPolicy policy, TradingLimitUsage usage) implements TradingLimitStore {

        @Override
        public Optional<TradingLimitPolicy> findPolicy(UUID userId) {
            return Optional.ofNullable(policy);
        }

        @Override
        public Optional<TradingLimitPolicy> findPolicyForUpdate(UUID userId) {
            return Optional.ofNullable(policy);
        }

        @Override
        public TradingLimitPolicy savePolicy(UUID userId, UpdateTradingLimitCommand command, BookingActor updatedBy) {
            throw new UnsupportedOperationException("Not needed for this test");
        }

        @Override
        public TradingLimitUsage usage(UUID userId, TradingLimitWindows windows) {
            return usage;
        }

        @Override
        public TradingLimitSnapshot snapshot(UUID userId, TradingLimitWindows windows) {
            throw new UnsupportedOperationException("Not needed for this test");
        }

        @Override
        public TradingLimitUserPage searchFoUsers(String query, int page, int size, TradingLimitWindows windows) {
            throw new UnsupportedOperationException("Not needed for this test");
        }

        @Override
        public boolean isActiveFoUser(UUID userId) {
            return true;
        }
    }
}
