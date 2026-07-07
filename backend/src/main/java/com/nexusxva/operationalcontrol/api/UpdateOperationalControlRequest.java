package com.nexusxva.operationalcontrol.api;

import com.nexusxva.operationalcontrol.domain.OperationalControlSettings;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.DateTimeException;
import java.util.EnumSet;
import java.util.List;

public record UpdateOperationalControlRequest(
        @NotBlank String timezone,
        @NotEmpty List<String> businessDays,
        @NotNull LocalTime tradingOpenTime,
        @NotNull LocalTime tradingCloseTime,
        boolean eodEnabled,
        @NotNull LocalTime eodRunTime,
        boolean eodAllowStaleMarketData
) {

    OperationalControlSettings toSettings() {
        EnumSet<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        try {
            businessDays.forEach(day -> days.add(DayOfWeek.valueOf(day.trim().toUpperCase())));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("businessDays contains an invalid day");
        }
        ZoneId zone;
        try {
            zone = ZoneId.of(timezone.trim());
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException("timezone must be a valid ZoneId");
        }
        return new OperationalControlSettings(
                zone,
                days,
                tradingOpenTime,
                tradingCloseTime,
                eodEnabled,
                eodRunTime,
                eodAllowStaleMarketData,
                Instant.now(),
                null,
                0
        );
    }
}
