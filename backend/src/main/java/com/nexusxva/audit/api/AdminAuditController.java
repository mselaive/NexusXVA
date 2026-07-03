package com.nexusxva.audit.api;

import com.nexusxva.audit.application.AuditSearchCriteria;
import com.nexusxva.audit.application.AuditService;
import com.nexusxva.audit.domain.AuditOutcome;
import com.nexusxva.shared.error.ResourceNotFoundException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit-events")
public class AdminAuditController {

    private final AuditService service;

    public AdminAuditController(AuditService service) {
        this.service = service;
    }

    @GetMapping
    public AuditEventPageResponse search(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) AuditOutcome outcome,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return AuditEventPageResponse.from(service.search(new AuditSearchCriteria(
                userId,
                username,
                module,
                eventType,
                outcome,
                resourceType,
                resourceId,
                from,
                to,
                page,
                size
        )));
    }

    @GetMapping("/{eventId}")
    public AuditEventResponse get(@PathVariable UUID eventId) {
        return service.find(eventId)
                .map(AuditEventResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Audit event not found"));
    }
}
