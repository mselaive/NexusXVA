package com.nexusxva.simulation.api;

import com.nexusxva.exposure.application.ExposureSimulationCommand;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record ExposureSimulationRequest(
        @NotNull UUID portfolioId,
        @NotNull LocalDate valuationDate,
        @NotNull @Min(1) @Max(3650) Integer horizonDays,
        @NotNull @Min(1) @Max(3650) Integer timeSteps,
        @NotNull @Min(1) @Max(100000) Integer paths,
        @NotNull Long seed,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) @DecimalMax(value = "1.0", inclusive = false)
        Double pfeConfidenceLevel
) {

    ExposureSimulationCommand toCommand() {
        return new ExposureSimulationCommand(
                portfolioId,
                valuationDate,
                horizonDays,
                timeSteps,
                paths,
                seed,
                pfeConfidenceLevel
        );
    }
}
