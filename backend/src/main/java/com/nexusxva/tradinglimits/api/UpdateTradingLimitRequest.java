package com.nexusxva.tradinglimits.api;

import com.nexusxva.tradinglimits.application.UpdateTradingLimitCommand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

public record UpdateTradingLimitRequest(
        @Min(1) Integer maxTradesPerHour,
        @Min(1) Integer maxTradesPerDay,
        @DecimalMin(value = "0.0", inclusive = false) BigDecimal maxNotionalPerHour,
        @DecimalMin(value = "0.0", inclusive = false) BigDecimal maxNotionalPerDay,
        boolean active,
        Long version
) {

    UpdateTradingLimitCommand toCommand() {
        return new UpdateTradingLimitCommand(
                maxTradesPerHour,
                maxTradesPerDay,
                maxNotionalPerHour,
                maxNotionalPerDay,
                active,
                version
        );
    }
}

