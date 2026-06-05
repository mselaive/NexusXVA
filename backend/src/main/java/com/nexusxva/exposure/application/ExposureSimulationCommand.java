package com.nexusxva.exposure.application;

import java.time.LocalDate;
import java.util.UUID;

public record ExposureSimulationCommand(
        UUID portfolioId,
        LocalDate valuationDate,
        int horizonDays,
        int timeSteps,
        int paths,
        long seed,
        double pfeConfidenceLevel
) {

    public ExposureSimulationCommand {
        if (portfolioId == null) {
            throw new IllegalArgumentException("portfolioId is required");
        }
        if (valuationDate == null) {
            throw new IllegalArgumentException("valuationDate is required");
        }
        if (horizonDays <= 0) {
            throw new IllegalArgumentException("horizonDays must be greater than zero");
        }
        if (timeSteps <= 0) {
            throw new IllegalArgumentException("timeSteps must be greater than zero");
        }
        if (timeSteps > horizonDays) {
            throw new IllegalArgumentException("timeSteps must be less than or equal to horizonDays");
        }
        if (paths <= 0) {
            throw new IllegalArgumentException("paths must be greater than zero");
        }
        if (!Double.isFinite(pfeConfidenceLevel) || pfeConfidenceLevel <= 0.0 || pfeConfidenceLevel >= 1.0) {
            throw new IllegalArgumentException("pfeConfidenceLevel must be between 0 and 1");
        }
    }
}
