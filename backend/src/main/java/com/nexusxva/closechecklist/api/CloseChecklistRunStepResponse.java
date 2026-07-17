package com.nexusxva.closechecklist.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexusxva.closechecklist.domain.CloseChecklistRunStep;
import com.nexusxva.closechecklist.domain.CloseChecklistStepStatus;
import com.nexusxva.operationalcontrol.domain.CloseChecklistPhase;
import com.nexusxva.operationalcontrol.domain.CloseChecklistStepType;
import java.time.Instant;
import java.util.UUID;

public record CloseChecklistRunStepResponse(
        UUID id,
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
    static CloseChecklistRunStepResponse from(CloseChecklistRunStep step) {
        return new CloseChecklistRunStepResponse(
                step.id(),
                step.phase(),
                step.stepType(),
                step.templateId(),
                step.scriptMode(),
                step.order(),
                step.critical(),
                step.status(),
                step.startedAt(),
                step.completedAt(),
                step.message(),
                step.output()
        );
    }
}
