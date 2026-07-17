package com.nexusxva.closechecklist.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexusxva.operationalcontrol.domain.CloseChecklistPhase;
import com.nexusxva.operationalcontrol.domain.CloseChecklistStepType;
import java.time.Instant;
import java.util.UUID;

public record CloseChecklistRunStep(
        UUID id,
        UUID runId,
        CloseChecklistPhase phase,
        CloseChecklistStepType stepType,
        UUID templateId,
        com.nexusxva.executescript.domain.ExecuteScriptMode scriptMode,
        int order,
        boolean critical,
        CloseChecklistStepStatus status,
        Instant startedAt,
        Instant completedAt,
        String message,
        JsonNode output
) {
}
