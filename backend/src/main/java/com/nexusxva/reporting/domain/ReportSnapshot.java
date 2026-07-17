package com.nexusxva.reporting.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ReportSnapshot(
        UUID id,
        ReportSnapshotType reportType,
        String title,
        LocalDate businessDate,
        ReportSnapshotScopeType scopeType,
        UUID scopeId,
        String scopeName,
        UUID requestedByUserId,
        String requestedByUsername,
        String requestedByDisplayName,
        String activeGroupCode,
        JsonNode filters,
        JsonNode result,
        JsonNode summary,
        Instant createdAt
) {
}
