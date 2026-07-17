package com.nexusxva.operationalcontrol.domain;

public record CloseChecklistStepDefinition(
        CloseChecklistPhase phase,
        CloseChecklistStepType stepType,
        java.util.UUID templateId,
        com.nexusxva.executescript.domain.ExecuteScriptMode scriptMode,
        boolean enabled,
        boolean critical,
        int order
) {
    public CloseChecklistStepDefinition(CloseChecklistPhase phase, CloseChecklistStepType stepType, boolean enabled, boolean critical, int order) {
        this(phase, stepType, null, null, enabled, critical, order);
    }

    public CloseChecklistStepDefinition {
        if (phase == null) {
            throw new IllegalArgumentException("close checklist step phase is required");
        }
        if (stepType == null) {
            throw new IllegalArgumentException("close checklist step type is required");
        }
        if (order <= 0) {
            throw new IllegalArgumentException("close checklist step order must be greater than zero");
        }
        if (stepType == CloseChecklistStepType.SCRIPT_TEMPLATE && templateId == null) {
            throw new IllegalArgumentException("close checklist script templateId is required");
        }
        if (stepType == CloseChecklistStepType.SCRIPT_TEMPLATE && scriptMode == null) {
            scriptMode = com.nexusxva.executescript.domain.ExecuteScriptMode.DRY_RUN;
        }
    }
}
