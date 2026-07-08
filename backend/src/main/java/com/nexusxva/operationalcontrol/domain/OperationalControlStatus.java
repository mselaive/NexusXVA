package com.nexusxva.operationalcontrol.domain;

import java.time.Instant;
import java.time.LocalTime;

public record OperationalControlStatus(
        boolean tradingOpen,
        String reason,
        String timezone,
        Instant currentBusinessTime,
        Instant nextOpenAt,
        LocalTime tradingOpenTime,
        LocalTime tradingCloseTime,
        boolean operationalWindowEnforced,
        boolean tradeBookingsWindowEnforced,
        boolean riskRunsWindowEnforced,
        boolean eodEnabled,
        Instant nextEodAt
) {
}
