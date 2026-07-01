package com.nexusxva.simulation.api;

import com.nexusxva.exposure.application.ExposureSimulationResult;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ExposureSimulationResponse(
        UUID portfolioId,
        LocalDate valuationDate,
        String model,
        String baseCurrency,
        int paths,
        int timeSteps,
        double pfeConfidenceLevel,
        List<ExposurePointResponse> points
) {

    static ExposureSimulationResponse from(ExposureSimulationResult result) {
        return new ExposureSimulationResponse(
                result.portfolioId(),
                result.valuationDate(),
                result.model(),
                result.baseCurrency(),
                result.paths(),
                result.timeSteps(),
                result.pfeConfidenceLevel(),
                result.points()
                        .stream()
                        .map(ExposurePointResponse::from)
                        .toList()
        );
    }
}
