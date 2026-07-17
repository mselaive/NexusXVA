package com.nexusxva.executescript.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record ExecuteScriptRunStep(
        UUID id,
        UUID runId,
        ExecuteScriptStepType stepType,
        int order,
        boolean critical,
        ExecuteScriptStepStatus status,
        Instant startedAt,
        Instant completedAt,
        String message,
        JsonNode output
) {
}
