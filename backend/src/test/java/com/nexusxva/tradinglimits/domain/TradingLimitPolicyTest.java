package com.nexusxva.tradinglimits.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TradingLimitPolicyTest {

    @Test
    void acceptsOptionalPositiveLimits() {
        TradingLimitPolicy policy = policy(2, 10, new BigDecimal("1000"), new BigDecimal("5000"));

        assertThat(policy.hasNotionalLimit()).isTrue();
        assertThat(policy.notionalCurrency()).isEqualTo("USD");
    }

    @Test
    void rejectsDailyTradeLimitBelowHourlyLimit() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> policy(5, 4, null, null))
                .withMessage("maxTradesPerDay must be greater than or equal to maxTradesPerHour");
    }

    @Test
    void rejectsDailyNotionalLimitBelowHourlyLimit() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> policy(null, null, new BigDecimal("5000"), new BigDecimal("4999")))
                .withMessage("maxNotionalPerDay must be greater than or equal to maxNotionalPerHour");
    }

    private TradingLimitPolicy policy(
            Integer maxTradesPerHour,
            Integer maxTradesPerDay,
            BigDecimal maxNotionalPerHour,
            BigDecimal maxNotionalPerDay
    ) {
        Instant now = Instant.parse("2026-06-19T15:42:10Z");
        return new TradingLimitPolicy(
                UUID.randomUUID(),
                maxTradesPerHour,
                maxTradesPerDay,
                maxNotionalPerHour,
                maxNotionalPerDay,
                "USD",
                true,
                now,
                now,
                UUID.randomUUID(),
                "bo-user",
                "BO User",
                0
        );
    }
}
