package com.nexusxva.closechecklist.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusxva.closechecklist.application.CloseChecklistStore;
import com.nexusxva.closechecklist.domain.CloseChecklistRun;
import com.nexusxva.closechecklist.domain.CloseChecklistRunStatus;
import com.nexusxva.closechecklist.domain.CloseChecklistRunStep;
import com.nexusxva.closechecklist.domain.CloseChecklistStepStatus;
import com.nexusxva.operationalcontrol.domain.CloseChecklistPhase;
import com.nexusxva.operationalcontrol.domain.CloseChecklistSettings;
import com.nexusxva.operationalcontrol.domain.CloseChecklistStepDefinition;
import com.nexusxva.operationalcontrol.domain.CloseChecklistStepType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcCloseChecklistStore implements CloseChecklistStore {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    JdbcCloseChecklistStore(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void startRun(UUID runId, LocalDate businessDate, String source, UUID requestedByUserId, CloseChecklistSettings settings, Instant startedAt) {
        jdbcTemplate.update(
                """
                INSERT INTO close_checklist_runs (id, business_date, source, status, started_at, requested_by_user_id, message, config_json)
                VALUES (?, ?, ?, 'RUNNING', ?, ?, ?, ?::jsonb)
                """,
                runId,
                businessDate,
                source,
                Timestamp.from(startedAt),
                requestedByUserId,
                "Close checklist started",
                toJson(settings)
        );
    }

    @Override
    public boolean tryStartScheduledRun(UUID runId, LocalDate businessDate, CloseChecklistSettings settings, Instant startedAt) {
        try {
            startRun(runId, businessDate, "SCHEDULED", null, settings, startedAt);
            return true;
        } catch (DuplicateKeyException exception) {
            return false;
        }
    }

    @Override
    public void createStep(UUID stepId, UUID runId, CloseChecklistStepDefinition definition) {
        jdbcTemplate.update(
                """
                INSERT INTO close_checklist_run_steps (
                    id, run_id, phase, step_type, step_order, critical, status, template_id, script_mode
                )
                VALUES (?, ?, ?, ?, ?, ?, 'PENDING', ?, ?)
                """,
                stepId,
                runId,
                definition.phase().name(),
                definition.stepType().name(),
                definition.order(),
                definition.critical(),
                definition.templateId(),
                definition.scriptMode() == null ? null : definition.scriptMode().name()
        );
    }

    @Override
    public void markStepRunning(UUID stepId, Instant startedAt) {
        jdbcTemplate.update(
                "UPDATE close_checklist_run_steps SET status = 'RUNNING', started_at = ? WHERE id = ?",
                Timestamp.from(startedAt),
                stepId
        );
    }

    @Override
    public void completeStep(UUID stepId, String message, Object output) {
        finishStep(stepId, CloseChecklistStepStatus.COMPLETED, message, output);
    }

    @Override
    public void failStep(UUID stepId, String message, Object output) {
        finishStep(stepId, CloseChecklistStepStatus.FAILED, message, output);
    }

    @Override
    public void skipStep(UUID stepId, String message, Object output) {
        finishStep(stepId, CloseChecklistStepStatus.SKIPPED, message, output);
    }

    @Override
    public void completeRun(UUID runId, CloseChecklistRunStatus status, String message) {
        jdbcTemplate.update(
                """
                UPDATE close_checklist_runs
                SET status = ?, completed_at = ?, message = ?
                WHERE id = ?
                """,
                status.name(),
                Timestamp.from(Instant.now()),
                truncate(message),
                runId
        );
    }

    @Override
    public Optional<CloseChecklistRun> find(UUID runId) {
        return jdbcTemplate.query(
                "SELECT * FROM close_checklist_runs WHERE id = ?",
                this::mapRun,
                runId
        ).stream().findFirst();
    }

    @Override
    public List<CloseChecklistRun> recent(int limit) {
        return jdbcTemplate.query(
                """
                SELECT *
                FROM close_checklist_runs
                ORDER BY started_at DESC
                LIMIT ?
                """,
                this::mapRun,
                Math.max(1, Math.min(limit, 100))
        );
    }

    @Override
    public Optional<LocalDate> latestScheduledBusinessDate() {
        return jdbcTemplate.query(
                """
                SELECT business_date
                FROM close_checklist_runs
                WHERE source = 'SCHEDULED'
                ORDER BY started_at DESC
                LIMIT 1
                """,
                (rs, rowNum) -> rs.getObject("business_date", LocalDate.class)
        ).stream().findFirst();
    }

    private void finishStep(UUID stepId, CloseChecklistStepStatus status, String message, Object output) {
        jdbcTemplate.update(
                """
                UPDATE close_checklist_run_steps
                SET status = ?, completed_at = ?, message = ?, output_json = ?::jsonb
                WHERE id = ?
                """,
                status.name(),
                Timestamp.from(Instant.now()),
                truncate(message),
                toJson(output == null ? java.util.Map.of() : output),
                stepId
        );
    }

    private CloseChecklistRun mapRun(ResultSet rs, int rowNum) throws SQLException {
        UUID runId = rs.getObject("id", UUID.class);
        return new CloseChecklistRun(
                runId,
                rs.getObject("business_date", LocalDate.class),
                rs.getString("source"),
                CloseChecklistRunStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("started_at").toInstant(),
                rs.getTimestamp("completed_at") == null ? null : rs.getTimestamp("completed_at").toInstant(),
                rs.getObject("requested_by_user_id", UUID.class),
                rs.getString("message"),
                readTree(rs.getString("config_json")),
                steps(runId)
        );
    }

    private List<CloseChecklistRunStep> steps(UUID runId) {
        return jdbcTemplate.query(
                """
                SELECT *
                FROM close_checklist_run_steps
                WHERE run_id = ?
                ORDER BY step_order
                """,
                this::mapStep,
                runId
        );
    }

    private CloseChecklistRunStep mapStep(ResultSet rs, int rowNum) throws SQLException {
        return new CloseChecklistRunStep(
                rs.getObject("id", UUID.class),
                rs.getObject("run_id", UUID.class),
                CloseChecklistPhase.valueOf(rs.getString("phase")),
                CloseChecklistStepType.valueOf(rs.getString("step_type")),
                rs.getObject("template_id", UUID.class),
                rs.getString("script_mode") == null ? null : com.nexusxva.executescript.domain.ExecuteScriptMode.valueOf(rs.getString("script_mode")),
                rs.getInt("step_order"),
                rs.getBoolean("critical"),
                CloseChecklistStepStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("started_at") == null ? null : rs.getTimestamp("started_at").toInstant(),
                rs.getTimestamp("completed_at") == null ? null : rs.getTimestamp("completed_at").toInstant(),
                rs.getString("message"),
                readTree(rs.getString("output_json"))
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Could not serialize close checklist data");
        }
    }

    private JsonNode readTree(String json) {
        try {
            return objectMapper.readTree(json == null ? "{}" : json);
        } catch (JsonProcessingException exception) {
            return objectMapper.createObjectNode();
        }
    }

    private String truncate(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
