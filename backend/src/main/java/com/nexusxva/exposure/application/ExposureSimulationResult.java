package com.nexusxva.exposure.application;

import com.nexusxva.exposure.domain.ExposurePoint;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ExposureSimulationResult(
        UUID portfolioId,
        LocalDate valuationDate,
        String model,
        int paths,
        int timeSteps,
        double pfeConfidenceLevel,
        List<ExposurePoint> points
) {

    public ExposureSimulationResult {
        if (portfolioId == null) {
            throw new IllegalArgumentException("portfolioId is required");
        }
        if (valuationDate == null) {
            throw new IllegalArgumentException("valuationDate is required");
        }
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("exposure model is required");
        }
        if (points == null) {
            throw new IllegalArgumentException("exposure points are required");
        }
        points = List.copyOf(points);
    }
}
