package com.nexusxva.audit.domain;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record AuditEvent(
        UUID id,
        Instant occurredAt,
        String eventType,
        String module,
        String action,
        AuditOutcome outcome,
        UUID actorUserId,
        String username,
        String displayName,
        String activeGroup,
        UUID sessionId,
        String httpMethod,
        String path,
        Integer statusCode,
        String resourceType,
        String resourceId,
        String correlationId,
        String ipAddress,
        String userAgent,
        String message,
        JsonNode metadata
) {
}
