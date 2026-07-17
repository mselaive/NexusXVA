package com.nexusxva.closechecklist.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexusxva.closechecklist.domain.CloseChecklistRun;
import com.nexusxva.closechecklist.domain.CloseChecklistRunStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CloseChecklistRunResponse(
        UUID id,
        LocalDate businessDate,
        String source,
        CloseChecklistRunStatus status,
        Instant startedAt,
        Instant completedAt,
        UUID requestedByUserId,
        String message,
        JsonNode config,
        List<CloseChecklistRunStepResponse> steps
) {
    static CloseChecklistRunResponse from(CloseChecklistRun run) {
        return new CloseChecklistRunResponse(
                run.id(),
                run.businessDate(),
                run.source(),
                run.status(),
                run.startedAt(),
                run.completedAt(),
                run.requestedByUserId(),
                run.message(),
                run.config(),
                run.steps().stream().map(CloseChecklistRunStepResponse::from).toList()
        );
    }
}
