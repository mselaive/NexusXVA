package com.nexusxva.operationalcontrol.domain;

import java.util.UUID;

public record CloseChecklistRiskDefaults(
        int horizonDays,
        int timeSteps,
        int paths,
        long seed,
        double pfeConfidenceLevel,
        double lossGivenDefault,
        UUID creditCurveId,
        UUID discountCurveId
) {
    public CloseChecklistRiskDefaults {
        if (horizonDays <= 0) {
            throw new IllegalArgumentException("horizonDays must be greater than zero");
        }
        if (timeSteps <= 0 || timeSteps > horizonDays) {
            throw new IllegalArgumentException("timeSteps must be greater than zero and less than or equal to horizonDays");
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
    }

    public static CloseChecklistRiskDefaults defaults() {
        return new CloseChecklistRiskDefaults(365, 12, 1000, 12345L, 0.95, 0.6, null, null);
    }
}
