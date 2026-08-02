package com.nexusxva.xva.api;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record ImportMarketCreditCurveRequest(
        @NotNull UUID counterpartyId,
        @NotNull LocalDate valuationDate,
        @NotNull @DecimalMin("0.0") @DecimalMax(value = "1.0", inclusive = false) Double recoveryRate,
        String name,
        boolean allowStale
) {}
