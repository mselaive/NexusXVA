package com.nexusxva.audit.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusxva.audit.application.AuditSearchCriteria;
import com.nexusxva.audit.application.AuditStore;
import com.nexusxva.audit.domain.AuditEvent;
import com.nexusxva.audit.domain.AuditOutcome;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcAuditStore implements AuditStore {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    JdbcAuditStore(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(AuditEvent event) {
        jdbcTemplate.update(
                """
                INSERT INTO audit_events (
                    id, occurred_at, event_type, module, action, outcome,
                    actor_user_id, username, display_name, active_group, session_id,
                    http_method, path, status_code, resource_type, resource_id,
                    correlation_id, ip_address, user_agent, message, metadata_json
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb))
                """,
                event.id(),
                Timestamp.from(event.occurredAt()),
                event.eventType(),
                event.module(),
                event.action(),
                event.outcome().name(),
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
                json(event.metadata())
        );
    }

    @Override
    public Optional<AuditEvent> find(UUID eventId) {
        List<AuditEvent> events = jdbcTemplate.query(
                """
                SELECT *
                FROM audit_events
                WHERE id = ?
                """,
                this::mapEvent,
                eventId
        );
        return events.stream().findFirst();
    }

    @Override
    public Page<AuditEvent> search(AuditSearchCriteria criteria) {
        StringBuilder sql = new StringBuilder("""
                SELECT *
                FROM audit_events
                WHERE 1 = 1
                """);
        StringBuilder countSql = new StringBuilder("""
                SELECT count(*)
                FROM audit_events
                WHERE 1 = 1
                """);
        List<Object> params = new ArrayList<>();
        List<Object> countParams = new ArrayList<>();

        appendFilters(sql, params, criteria);
        appendFilters(countSql, countParams, criteria);

        sql.append(" ORDER BY occurred_at DESC LIMIT ? OFFSET ?");
        params.add(criteria.size());
        params.add((long) criteria.page() * criteria.size());

        List<AuditEvent> events = jdbcTemplate.query(sql.toString(), this::mapEvent, params.toArray());
        Long total = jdbcTemplate.queryForObject(countSql.toString(), Long.class, countParams.toArray());
        return new PageImpl<>(events, PageRequest.of(criteria.page(), criteria.size()), total == null ? 0 : total);
    }

    private void appendFilters(StringBuilder sql, List<Object> params, AuditSearchCriteria criteria) {
        if (criteria.userId() != null) {
            sql.append(" AND actor_user_id = ?");
            params.add(criteria.userId());
        }
        if (criteria.username() != null && !criteria.username().isBlank()) {
            sql.append(" AND lower(username) LIKE lower(?)");
            params.add("%" + criteria.username().trim() + "%");
        }
        if (criteria.module() != null && !criteria.module().isBlank()) {
            sql.append(" AND module = ?");
            params.add(criteria.module().trim().toUpperCase());
        }
        if (criteria.eventType() != null && !criteria.eventType().isBlank()) {
            sql.append(" AND event_type = ?");
            params.add(criteria.eventType().trim().toUpperCase());
        }
        if (criteria.outcome() != null) {
            sql.append(" AND outcome = ?");
            params.add(criteria.outcome().name());
        }
        if (criteria.resourceType() != null && !criteria.resourceType().isBlank()) {
            sql.append(" AND resource_type = ?");
            params.add(criteria.resourceType().trim().toUpperCase());
        }
        if (criteria.resourceId() != null && !criteria.resourceId().isBlank()) {
            sql.append(" AND resource_id = ?");
            params.add(criteria.resourceId().trim());
        }
        if (criteria.from() != null) {
            sql.append(" AND occurred_at >= ?");
            params.add(Timestamp.from(criteria.from()));
        }
        if (criteria.to() != null) {
            sql.append(" AND occurred_at <= ?");
            params.add(Timestamp.from(criteria.to()));
        }
    }

    private AuditEvent mapEvent(ResultSet rs, int rowNum) throws SQLException {
        return new AuditEvent(
                rs.getObject("id", UUID.class),
                rs.getTimestamp("occurred_at").toInstant(),
                rs.getString("event_type"),
                rs.getString("module"),
                rs.getString("action"),
                AuditOutcome.valueOf(rs.getString("outcome")),
                rs.getObject("actor_user_id", UUID.class),
                rs.getString("username"),
                rs.getString("display_name"),
                rs.getString("active_group"),
                rs.getObject("session_id", UUID.class),
                rs.getString("http_method"),
                rs.getString("path"),
                (Integer) rs.getObject("status_code"),
                rs.getString("resource_type"),
                rs.getString("resource_id"),
                rs.getString("correlation_id"),
                rs.getString("ip_address"),
                rs.getString("user_agent"),
                rs.getString("message"),
                parseJson(rs.getString("metadata_json"))
        );
    }

    private JsonNode parseJson(String value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Stored audit metadata is unreadable", exception);
        }
    }

    private String json(JsonNode node) {
        return node == null ? null : node.toString();
    }
}
