package com.nexusxva.reporting.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusxva.reporting.application.ReportSnapshotSearchCriteria;
import com.nexusxva.reporting.application.ReportSnapshotStore;
import com.nexusxva.reporting.domain.ReportSnapshot;
import com.nexusxva.reporting.domain.ReportSnapshotScopeType;
import com.nexusxva.reporting.domain.ReportSnapshotType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcReportSnapshotStore implements ReportSnapshotStore {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    JdbcReportSnapshotStore(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public ReportSnapshot save(ReportSnapshot snapshot) {
        jdbcTemplate.update(
                """
                INSERT INTO report_snapshots (
                    id, report_type, title, business_date, scope_type, scope_id, scope_name_snapshot,
                    requested_by_user_id, requested_by_username, requested_by_display_name, active_group_code,
                    filters_json, result_json, summary_json, created_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), CAST(? AS jsonb), ?)
                """,
                snapshot.id(),
                snapshot.reportType().name(),
                snapshot.title(),
                snapshot.businessDate(),
                snapshot.scopeType().name(),
                snapshot.scopeId(),
                snapshot.scopeName(),
                snapshot.requestedByUserId(),
                snapshot.requestedByUsername(),
                snapshot.requestedByDisplayName(),
                snapshot.activeGroupCode(),
                json(snapshot.filters()),
                json(snapshot.result()),
                json(snapshot.summary()),
                Timestamp.from(snapshot.createdAt())
        );
        return find(snapshot.id()).orElse(snapshot);
    }

    @Override
    public Optional<ReportSnapshot> find(UUID snapshotId) {
        return jdbcTemplate.query("SELECT * FROM report_snapshots WHERE id = ?", this::mapSnapshot, snapshotId)
                .stream()
                .findFirst();
    }

    @Override
    public List<ReportSnapshot> search(ReportSnapshotSearchCriteria criteria) {
        StringBuilder sql = new StringBuilder("SELECT * FROM report_snapshots WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (criteria.reportType() != null) {
            sql.append(" AND report_type = ?");
            params.add(criteria.reportType().name());
        }
        if (criteria.requestedByUserId() != null) {
            sql.append(" AND requested_by_user_id = ?");
            params.add(criteria.requestedByUserId());
        }
        if (criteria.activeGroupCode() != null && !criteria.activeGroupCode().isBlank()) {
            sql.append(" AND active_group_code = ?");
            params.add(criteria.activeGroupCode());
        }
        sql.append(" ORDER BY created_at DESC LIMIT ?");
        params.add(criteria.limit());
        return jdbcTemplate.query(sql.toString(), this::mapSnapshot, params.toArray());
    }

    private ReportSnapshot mapSnapshot(ResultSet rs, int rowNum) throws SQLException {
        return new ReportSnapshot(
                rs.getObject("id", UUID.class),
                ReportSnapshotType.valueOf(rs.getString("report_type")),
                rs.getString("title"),
                rs.getObject("business_date", LocalDate.class),
                ReportSnapshotScopeType.valueOf(rs.getString("scope_type")),
                rs.getObject("scope_id", UUID.class),
                rs.getString("scope_name_snapshot"),
                rs.getObject("requested_by_user_id", UUID.class),
                rs.getString("requested_by_username"),
                rs.getString("requested_by_display_name"),
                rs.getString("active_group_code"),
                parseJson(rs.getString("filters_json")),
                parseJson(rs.getString("result_json")),
                parseJson(rs.getString("summary_json")),
                rs.getTimestamp("created_at").toInstant()
        );
    }

    private JsonNode parseJson(String value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Stored report snapshot JSON is unreadable", exception);
        }
    }

    private String json(JsonNode node) {
        return node == null ? null : node.toString();
    }
}
