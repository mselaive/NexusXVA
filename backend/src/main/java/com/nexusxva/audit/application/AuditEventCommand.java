package com.nexusxva.audit.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.nexusxva.audit.domain.AuditOutcome;
import com.nexusxva.auth.domain.AuthSession;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

public record AuditEventCommand(
        String eventType,
        String module,
        String action,
        AuditOutcome outcome,
        AuthSession session,
        HttpServletRequest request,
        Integer statusCode,
        String resourceType,
        String resourceId,
        String message,
        JsonNode metadata
) {
    public static AuditEventCommand of(
            String eventType,
            String module,
            String action,
            AuditOutcome outcome,
            AuthSession session,
            HttpServletRequest request,
            Integer statusCode,
            String resourceType,
            UUID resourceId,
            String message,
            JsonNode metadata
    ) {
        return new AuditEventCommand(
                eventType,
                module,
                action,
                outcome,
                session,
                request,
                statusCode,
                resourceType,
                resourceId == null ? null : resourceId.toString(),
                message,
                metadata
        );
    }
}
