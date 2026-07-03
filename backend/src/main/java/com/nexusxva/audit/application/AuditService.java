package com.nexusxva.audit.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusxva.audit.domain.AuditEvent;
import com.nexusxva.audit.domain.AuditOutcome;
import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.infrastructure.AuthSessionFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    public static final String CORRELATION_ID_ATTRIBUTE = "nexusxva.audit.correlationId";

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditService.class);

    private final AuditStore store;
    private final ObjectMapper objectMapper;

    public AuditService(AuditStore store, ObjectMapper objectMapper) {
        this.store = store;
        this.objectMapper = objectMapper;
    }

    public JsonNode metadata(Map<String, ?> values) {
        return values == null || values.isEmpty() ? null : objectMapper.valueToTree(values);
    }

    public void record(AuditEventCommand command) {
        try {
            store.save(toEvent(command));
        } catch (Exception exception) {
            LOGGER.warn(
                    "Audit event persistence failed eventType={} module={} action={} reason={}",
                    command.eventType(),
                    command.module(),
                    command.action(),
                    exception.getMessage()
            );
        }
    }

    public void recordDenied(HttpServletRequest request, int statusCode, String message) {
        record(new AuditEventCommand(
                "ACCESS_DENIED",
                moduleFromPath(request.getRequestURI()),
                request.getMethod() + " " + request.getRequestURI(),
                AuditOutcome.DENIED,
                currentSession(request),
                request,
                statusCode,
                null,
                null,
                message,
                null
        ));
    }

    @Transactional(readOnly = true)
    public Page<AuditEvent> search(AuditSearchCriteria criteria) {
        return store.search(criteria);
    }

    @Transactional(readOnly = true)
    public Optional<AuditEvent> find(UUID eventId) {
        return store.find(eventId);
    }

    private AuditEvent toEvent(AuditEventCommand command) {
        AuthSession session = command.session();
        HttpServletRequest request = command.request();
        return new AuditEvent(
                UUID.randomUUID(),
                Instant.now(),
                truncate(required(command.eventType(), "AUDIT_EVENT"), 80),
                truncate(required(command.module(), "UNKNOWN"), 80),
                truncate(required(command.action(), "UNKNOWN"), 120),
                command.outcome() == null ? AuditOutcome.SUCCESS : command.outcome(),
                session == null ? null : session.user().id(),
                session == null ? null : truncate(session.user().username(), 120),
                session == null ? null : truncate(session.user().displayName(), 200),
                session == null ? null : truncate(session.activeGroup(), 30),
                session == null ? null : session.id(),
                request == null ? null : truncate(request.getMethod(), 12),
                request == null ? null : truncate(request.getRequestURI(), 500),
                command.statusCode(),
                truncate(command.resourceType(), 80),
                truncate(command.resourceId(), 120),
                correlationId(request),
                ipAddress(request),
                request == null ? null : truncate(request.getHeader("User-Agent"), 500),
                truncate(command.message(), 500),
                command.metadata()
        );
    }

    private AuthSession currentSession(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object value = request.getAttribute(AuthSessionFilter.SESSION_ATTRIBUTE);
        return value instanceof AuthSession session ? session : null;
    }

    private String correlationId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object value = request.getAttribute(CORRELATION_ID_ATTRIBUTE);
        return value instanceof String correlationId ? truncate(correlationId, 80) : null;
    }

    private String ipAddress(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return truncate(forwarded.split(",")[0].trim(), 80);
        }
        return truncate(request.getRemoteAddr(), 80);
    }

    private String moduleFromPath(String path) {
        if (path == null || path.isBlank()) {
            return "UNKNOWN";
        }
        if (path.startsWith("/api/front-office")) {
            return "FRONT_OFFICE";
        }
        if (path.startsWith("/api/back-office")) {
            return "BACK_OFFICE";
        }
        if (path.startsWith("/api/admin")) {
            return "ADMIN";
        }
        if (path.startsWith("/api/auth")) {
            return "AUTH";
        }
        if (path.startsWith("/api/risk") || path.startsWith("/api/xva")) {
            return "XVA";
        }
        if (path.startsWith("/api/portfolios")) {
            return "PORTFOLIO";
        }
        return "API";
    }

    private String required(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.replace('\n', ' ').replace('\r', ' ').trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }
}
