package com.nexusxva.audit.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexusxva.audit.domain.AuditEvent;
import com.nexusxva.audit.domain.AuditOutcome;
import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
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
    static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(
                event.id(),
                event.occurredAt(),
                event.eventType(),
                event.module(),
                event.action(),
                event.outcome(),
                event.actorUserId(),
                event.username(),
                event.displayName(),
                event.activeGroup(),
                event.sessionId(),
                event.httpMethod(),
                event.path(),
                event.statusCode(),
                event.resourceType(),
                event.resourceId(),
                event.correlationId(),
                event.ipAddress(),
                event.userAgent(),
                event.message(),
                event.metadata()
        );
    }
}
