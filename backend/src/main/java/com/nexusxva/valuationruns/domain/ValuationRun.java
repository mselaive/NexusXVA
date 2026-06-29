package com.nexusxva.valuationruns.domain;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ValuationRun(
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
}
