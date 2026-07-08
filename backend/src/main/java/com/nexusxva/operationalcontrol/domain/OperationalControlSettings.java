package com.nexusxva.operationalcontrol.domain;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public record OperationalControlSettings(
        ZoneId timezone,
        Set<DayOfWeek> businessDays,
        LocalTime tradingOpenTime,
        LocalTime tradingCloseTime,
        boolean blockTradeBookingsOutsideWindow,
        boolean blockRiskRunsOutsideWindow,
        boolean eodEnabled,
        LocalTime eodRunTime,
        boolean eodAllowStaleMarketData,
        Instant updatedAt,
        UUID updatedByUserId,
        long version
) {

    public OperationalControlSettings {
        if (timezone == null) {
            throw new IllegalArgumentException("timezone is required");
        }
        if (businessDays == null || businessDays.isEmpty()) {
            throw new IllegalArgumentException("businessDays must contain at least one day");
        }
        if (tradingOpenTime == null || tradingCloseTime == null || eodRunTime == null) {
            throw new IllegalArgumentException("trading and EOD times are required");
        }
        if (!tradingOpenTime.isBefore(tradingCloseTime)) {
            throw new IllegalArgumentException("tradingOpenTime must be before tradingCloseTime");
        }
        if (!eodRunTime.isAfter(tradingCloseTime)) {
            throw new IllegalArgumentException("eodRunTime must be after tradingCloseTime");
        }
        businessDays = EnumSet.copyOf(businessDays);
    }

    public boolean enforceOperationalWindow() {
        return blockTradeBookingsOutsideWindow || blockRiskRunsOutsideWindow;
    }
}
