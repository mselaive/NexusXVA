package com.nexusxva.operationalcontrol.api;

import com.nexusxva.operationalcontrol.domain.OperationalControlStatus;
import java.time.Instant;
import java.time.LocalTime;

public record OperationalControlStatusResponse(
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

    static OperationalControlStatusResponse from(OperationalControlStatus status) {
        return new OperationalControlStatusResponse(
                status.tradingOpen(),
                status.reason(),
                status.timezone(),
                status.currentBusinessTime(),
                status.nextOpenAt(),
                status.tradingOpenTime(),
                status.tradingCloseTime(),
                status.operationalWindowEnforced(),
                status.tradeBookingsWindowEnforced(),
                status.riskRunsWindowEnforced(),
                status.eodEnabled(),
                status.nextEodAt()
        );
    }
}
