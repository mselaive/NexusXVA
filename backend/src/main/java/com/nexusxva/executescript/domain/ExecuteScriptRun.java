package com.nexusxva.executescript.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ExecuteScriptRun(
        UUID id,
        UUID templateId,
        String templateName,
        ExecuteScriptMode mode,
        LocalDate businessDate,
        ExecuteScriptRunStatus status,
        Instant startedAt,
        Instant completedAt,
        UUID requestedByUserId,
        String message,
        JsonNode input,
        List<ExecuteScriptRunStep> steps
) {
}
