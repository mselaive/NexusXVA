package com.nexusxva.reporting.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.reporting.domain.ReportSnapshot;
import com.nexusxva.reporting.domain.ReportSnapshotScopeType;
import com.nexusxva.reporting.domain.ReportSnapshotType;
import com.nexusxva.shared.error.ResourceNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportSnapshotService {

    private final ReportSnapshotStore store;
    private final ObjectMapper objectMapper;

    public ReportSnapshotService(ReportSnapshotStore store, ObjectMapper objectMapper) {
        this.store = store;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReportSnapshot record(
            AuthSession session,
            ReportSnapshotType reportType,
            String title,
            LocalDate businessDate,
            ReportSnapshotScopeType scopeType,
            UUID scopeId,
            String scopeName,
            Object filters,
            Object result,
            Object summary
    ) {
        return store.save(new ReportSnapshot(
                UUID.randomUUID(),
                reportType,
                title,
                businessDate,
                scopeType,
                scopeId,
                scopeName,
                userId(session),
                username(session),
                displayName(session),
                activeGroup(session),
                toJson(filters == null ? Map.of() : filters),
                toJson(result),
                toJson(summary == null ? Map.of() : summary),
                Instant.now()
        ));
    }

    @Transactional(readOnly = true)
    public ReportSnapshot get(UUID snapshotId) {
        return store.find(snapshotId)
                .orElseThrow(() -> new ResourceNotFoundException("Report snapshot not found"));
    }

    @Transactional(readOnly = true)
    public List<ReportSnapshot> search(ReportSnapshotSearchCriteria criteria) {
        return store.search(criteria);
    }

    private JsonNode toJson(Object value) {
        return objectMapper.valueToTree(value);
    }

    private UUID userId(AuthSession session) {
        return session == null ? null : session.user().id();
    }

    private String username(AuthSession session) {
        return session == null ? null : session.user().username();
    }

    private String displayName(AuthSession session) {
        return session == null ? null : session.user().displayName();
    }

    private String activeGroup(AuthSession session) {
        return session == null ? null : session.activeGroup();
    }
}
