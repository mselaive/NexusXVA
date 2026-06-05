package com.nexusxva.cva.api;

import com.nexusxva.cva.application.CvaCalculationCommand;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CvaCalculationRequest(
        @NotNull UUID portfolioId,
        @NotNull LocalDate valuationDate,
        @NotNull @Min(1) @Max(3650) Integer horizonDays,
        @NotNull @Min(1) @Max(3650) Integer timeSteps,
        @NotNull @Min(1) @Max(100000) Integer paths,
        @NotNull Long seed,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) @DecimalMax(value = "1.0", inclusive = false)
        Double pfeConfidenceLevel,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0")
        Double lossGivenDefault,
        @NotNull @DecimalMin("0.0")
        Double counterpartyHazardRate,
        @NotNull
        Double discountRate
) {

    CvaCalculationCommand toCommand() {
        return new CvaCalculationCommand(
                portfolioId,
                valuationDate,
                horizonDays,
                timeSteps,
                paths,
                seed,
                pfeConfidenceLevel,
                lossGivenDefault,
                counterpartyHazardRate,
                discountRate
        );
    }
}
