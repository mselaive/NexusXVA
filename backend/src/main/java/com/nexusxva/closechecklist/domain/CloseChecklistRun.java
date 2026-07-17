package com.nexusxva.closechecklist.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CloseChecklistRun(
        UUID id,
        LocalDate businessDate,
        String source,
        CloseChecklistRunStatus status,
        Instant startedAt,
        Instant completedAt,
        UUID requestedByUserId,
        String message,
        JsonNode config,
        List<CloseChecklistRunStep> steps
) {
}
