package com.nexusxva.tradinglimits.domain;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public record TradingLimitWindows(
        Instant hourStartsAt,
        Instant hourEndsAt,
        Instant dayStartsAt,
        Instant dayEndsAt
) {

    public static TradingLimitWindows at(Instant instant) {
        ZonedDateTime utc = instant.atZone(ZoneOffset.UTC);
        ZonedDateTime hourStart = utc.withMinute(0).withSecond(0).withNano(0);
        ZonedDateTime dayStart = utc.toLocalDate().atStartOfDay(ZoneOffset.UTC);
        return new TradingLimitWindows(
                hourStart.toInstant(),
                hourStart.plusHours(1).toInstant(),
                dayStart.toInstant(),
                dayStart.plusDays(1).toInstant()
        );
    }
}

