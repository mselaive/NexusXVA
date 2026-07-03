package com.nexusxva.audit.api;

import com.nexusxva.audit.domain.AuditEvent;
import java.util.List;
import org.springframework.data.domain.Page;

public record AuditEventPageResponse(
        List<AuditEventResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    static AuditEventPageResponse from(Page<AuditEvent> page) {
        return new AuditEventPageResponse(
                page.getContent().stream().map(AuditEventResponse::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
