package com.nexusxva.audit.application;

import com.nexusxva.audit.domain.AuditOutcome;
import java.time.Instant;
import java.util.UUID;

public record AuditSearchCriteria(
        UUID userId,
        String username,
        String module,
        String eventType,
        AuditOutcome outcome,
        String resourceType,
        String resourceId,
        Instant from,
        Instant to,
        int page,
        int size
) {
    public AuditSearchCriteria {
        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), 100);
    }
}
