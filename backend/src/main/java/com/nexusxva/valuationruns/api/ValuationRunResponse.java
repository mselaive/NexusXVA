package com.nexusxva.valuationruns.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexusxva.valuationruns.domain.ValuationRun;
import com.nexusxva.valuationruns.domain.ValuationRunStatus;
import com.nexusxva.valuationruns.domain.ValuationRunType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ValuationRunResponse(
        UUID id,
        UUID portfolioId,
        String portfolioName,
        ValuationRunType runType,
        String model,
        LocalDate valuationDate,
        ValuationRunStatus status,
        UUID requestedByUserId,
        String requestedByUsername,
        String requestedByDisplayName,
        String activeGroupCode,
        JsonNode input,
        JsonNode result,
        JsonNode summary,
        String errorMessage,
        Instant createdAt
) {

    static ValuationRunResponse from(ValuationRun run) {
        return new ValuationRunResponse(
                run.id(),
                run.portfolioId(),
                run.portfolioName(),
                run.runType(),
                run.model(),
                run.valuationDate(),
                run.status(),
                run.requestedByUserId(),
                run.requestedByUsername(),
                run.requestedByDisplayName(),
                run.activeGroupCode(),
                run.input(),
                run.result(),
                run.summary(),
                run.errorMessage(),
                run.createdAt()
        );
    }
}
