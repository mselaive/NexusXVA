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
        boolean eodEnabled,
        Instant nextEodAt
) {
}
