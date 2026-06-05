package com.nexusxva.cva.application;

import java.time.LocalDate;
import java.util.UUID;

public record CvaCalculationCommand(
        UUID portfolioId,
        LocalDate valuationDate,
        int horizonDays,
        int timeSteps,
        int paths,
        long seed,
        double pfeConfidenceLevel,
        double lossGivenDefault,
        double counterpartyHazardRate,
        double discountRate
) {

    public CvaCalculationCommand {
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
        if (!Double.isFinite(lossGivenDefault) || lossGivenDefault < 0.0 || lossGivenDefault > 1.0) {
            throw new IllegalArgumentException("lossGivenDefault must be between 0 and 1");
        }
        if (!Double.isFinite(counterpartyHazardRate) || counterpartyHazardRate < 0.0) {
            throw new IllegalArgumentException("counterpartyHazardRate must be greater than or equal to zero");
        }
        if (!Double.isFinite(discountRate)) {
            throw new IllegalArgumentException("discountRate must be finite");
        }
    }
}
