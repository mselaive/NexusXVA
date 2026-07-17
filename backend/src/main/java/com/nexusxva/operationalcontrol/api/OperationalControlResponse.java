package com.nexusxva.operationalcontrol.api;

import com.nexusxva.operationalcontrol.domain.OperationalControlSettings;
import com.nexusxva.operationalcontrol.domain.CloseChecklistSettings;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

public record OperationalControlResponse(
        String timezone,
        List<String> businessDays,
        LocalTime tradingOpenTime,
        LocalTime tradingCloseTime,
        boolean enforceOperationalWindow,
        boolean blockTradeBookingsOutsideWindow,
        boolean blockRiskRunsOutsideWindow,
        boolean eodEnabled,
        LocalTime eodRunTime,
        boolean eodAllowStaleMarketData,
        CloseChecklistSettings closeChecklist,
        Instant updatedAt,
        String updatedByUserId,
        long version
) {

    static OperationalControlResponse from(OperationalControlSettings settings) {
        return new OperationalControlResponse(
                settings.timezone().getId(),
                settings.businessDays().stream().sorted().map(Enum::name).toList(),
                settings.tradingOpenTime(),
                settings.tradingCloseTime(),
                settings.enforceOperationalWindow(),
                settings.blockTradeBookingsOutsideWindow(),
                settings.blockRiskRunsOutsideWindow(),
                settings.eodEnabled(),
                settings.eodRunTime(),
                settings.eodAllowStaleMarketData(),
                settings.closeChecklist(),
                settings.updatedAt(),
                settings.updatedByUserId() == null ? null : settings.updatedByUserId().toString(),
                settings.version()
        );
    }
}
