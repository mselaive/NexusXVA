package com.nexusxva.xva.api;

import com.nexusxva.xva.application.UpdateNettingSetCollateralCommand;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record UpdateNettingSetCollateralRequest(@NotNull @PositiveOrZero BigDecimal collateralAmount) {

    UpdateNettingSetCollateralCommand toCommand() {
        return new UpdateNettingSetCollateralCommand(collateralAmount);
    }
}
