package com.nexusxva.xva.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ImportMarketDiscountCurveRequest(
        @NotBlank String currency,
        @NotNull LocalDate valuationDate,
        String name,
        boolean allowStale
) {}
