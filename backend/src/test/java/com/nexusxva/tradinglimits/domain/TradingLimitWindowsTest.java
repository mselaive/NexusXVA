package com.nexusxva.tradinglimits.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class TradingLimitWindowsTest {

    @Test
    void buildsCalendarHourAndDayInUtc() {
        TradingLimitWindows windows = TradingLimitWindows.at(Instant.parse("2026-06-19T23:42:10.123Z"));

        assertThat(windows.hourStartsAt()).isEqualTo(Instant.parse("2026-06-19T23:00:00Z"));
        assertThat(windows.hourEndsAt()).isEqualTo(Instant.parse("2026-06-20T00:00:00Z"));
        assertThat(windows.dayStartsAt()).isEqualTo(Instant.parse("2026-06-19T00:00:00Z"));
        assertThat(windows.dayEndsAt()).isEqualTo(Instant.parse("2026-06-20T00:00:00Z"));
    }
}
