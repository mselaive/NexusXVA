package com.nexusxva.xva.application;

import java.math.BigDecimal;

public record UpdateNettingSetCollateralCommand(BigDecimal collateralAmount) {
    public UpdateNettingSetCollateralCommand {
        if (collateralAmount == null) {
            throw new IllegalArgumentException("collateralAmount is required");
        }
        if (collateralAmount.signum() < 0) {
            throw new IllegalArgumentException("collateralAmount must be greater than or equal to zero");
        }
    }
}
