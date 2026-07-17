package com.nexusxva.reporting.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexusxva.reporting.domain.ReportSnapshot;
import com.nexusxva.reporting.domain.ReportSnapshotScopeType;
import com.nexusxva.reporting.domain.ReportSnapshotType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ReportSnapshotResponse(
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
    static ReportSnapshotResponse from(ReportSnapshot snapshot) {
        return new ReportSnapshotResponse(
                snapshot.id(),
                snapshot.reportType(),
                snapshot.title(),
                snapshot.businessDate(),
                snapshot.scopeType(),
                snapshot.scopeId(),
                snapshot.scopeName(),
                snapshot.requestedByUserId(),
                snapshot.requestedByUsername(),
                snapshot.requestedByDisplayName(),
                snapshot.activeGroupCode(),
                snapshot.filters(),
                snapshot.result(),
                snapshot.summary(),
                snapshot.createdAt()
        );
    }
}
