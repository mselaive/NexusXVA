package com.nexusxva.executescript.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusxva.executescript.application.ExecuteScriptStore;
import com.nexusxva.executescript.application.SaveExecuteScriptTemplateCommand;
import com.nexusxva.executescript.domain.ExecuteScriptMode;
import com.nexusxva.executescript.domain.ExecuteScriptRun;
import com.nexusxva.executescript.domain.ExecuteScriptRunStatus;
import com.nexusxva.executescript.domain.ExecuteScriptRunStep;
import com.nexusxva.executescript.domain.ExecuteScriptStepStatus;
import com.nexusxva.executescript.domain.ExecuteScriptStepType;
import com.nexusxva.executescript.domain.ExecuteScriptTemplate;
import com.nexusxva.executescript.domain.ExecuteScriptTemplateStep;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcExecuteScriptStore implements ExecuteScriptStore {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    JdbcExecuteScriptStore(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public ExecuteScriptTemplate createTemplate(UUID templateId, SaveExecuteScriptTemplateCommand command, UUID updatedByUserId, Instant now) {
        jdbcTemplate.update(
                """
                INSERT INTO execute_script_templates (
                    id, name, description, active, default_parameters_json, created_at, updated_at, updated_by_user_id
                )
                VALUES (?, ?, ?, ?, ?::jsonb, ?, ?, ?)
                """,
                templateId,
                command.name(),
                command.description(),
                command.active(),
                toJson(command.defaultParameters()),
                Timestamp.from(now),
                Timestamp.from(now),
                updatedByUserId
        );
        replaceSteps(templateId, command.steps());
        return findTemplate(templateId).orElseThrow();
    }

    @Override
    public ExecuteScriptTemplate updateTemplate(UUID templateId, SaveExecuteScriptTemplateCommand command, UUID updatedByUserId, Instant now) {
        jdbcTemplate.update(
                """
                UPDATE execute_script_templates
                SET name = ?, description = ?, active = ?, default_parameters_json = ?::jsonb, updated_at = ?, updated_by_user_id = ?
                WHERE id = ?
                """,
                command.name(),
                command.description(),
                command.active(),
                toJson(command.defaultParameters()),
                Timestamp.from(now),
                updatedByUserId,
                templateId
        );
        jdbcTemplate.update("DELETE FROM execute_script_template_steps WHERE template_id = ?", templateId);
        replaceSteps(templateId, command.steps());
        return findTemplate(templateId).orElseThrow();
    }

    @Override
    public Optional<ExecuteScriptTemplate> findTemplate(UUID templateId) {
        return jdbcTemplate.query(
                "SELECT * FROM execute_script_templates WHERE id = ?",
                this::mapTemplate,
                templateId
        ).stream().findFirst();
    }

    @Override
    public List<ExecuteScriptTemplate> listTemplates(boolean includeInactive) {
        String sql = includeInactive
                ? "SELECT * FROM execute_script_templates ORDER BY updated_at DESC"
                : "SELECT * FROM execute_script_templates WHERE active = TRUE ORDER BY updated_at DESC";
        return jdbcTemplate.query(sql, this::mapTemplate);
    }

    @Override
    public void startRun(UUID runId, ExecuteScriptTemplate template, ExecuteScriptMode mode, LocalDate businessDate, UUID requestedByUserId, JsonNode input, Instant startedAt) {
        jdbcTemplate.update(
                """
                INSERT INTO execute_script_runs (
                    id, template_id, template_name, mode, business_date, status, started_at, requested_by_user_id, message, input_json
                )
                VALUES (?, ?, ?, ?, ?, 'RUNNING', ?, ?, ?, ?::jsonb)
                """,
                runId,
                template.id(),
                template.name(),
                mode.name(),
                businessDate,
                Timestamp.from(startedAt),
                requestedByUserId,
                "ExecuteScript started",
                toJson(input)
        );
    }

    @Override
    public void createRunStep(UUID stepId, UUID runId, ExecuteScriptTemplateStep step) {
        jdbcTemplate.update(
                """
                INSERT INTO execute_script_run_steps (
                    id, run_id, step_type, step_order, critical, status
                )
                VALUES (?, ?, ?, ?, ?, 'PENDING')
                """,
                stepId,
                runId,
                step.stepType().name(),
                step.order(),
                step.critical()
        );
    }

    @Override
    public void markStepRunning(UUID stepId, Instant startedAt) {
        jdbcTemplate.update(
                "UPDATE execute_script_run_steps SET status = 'RUNNING', started_at = ? WHERE id = ?",
                Timestamp.from(startedAt),
                stepId
        );
    }

    @Override
    public void finishStep(UUID stepId, ExecuteScriptStepStatus status, String message, Object output) {
        jdbcTemplate.update(
                """
                UPDATE execute_script_run_steps
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

    @Override
    public void completeRun(UUID runId, ExecuteScriptRunStatus status, String message) {
        jdbcTemplate.update(
                """
                UPDATE execute_script_runs
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
    public Optional<ExecuteScriptRun> findRun(UUID runId) {
        return jdbcTemplate.query(
                "SELECT * FROM execute_script_runs WHERE id = ?",
                this::mapRun,
                runId
        ).stream().findFirst();
    }

    @Override
    public List<ExecuteScriptRun> recentRuns(int limit) {
        return jdbcTemplate.query(
                """
                SELECT *
                FROM execute_script_runs
                ORDER BY started_at DESC
                LIMIT ?
                """,
                this::mapRun,
                Math.max(1, Math.min(limit, 100))
        );
    }

    private void replaceSteps(UUID templateId, List<SaveExecuteScriptTemplateCommand.Step> steps) {
        for (SaveExecuteScriptTemplateCommand.Step step : steps) {
            jdbcTemplate.update(
                    """
                    INSERT INTO execute_script_template_steps (
                        id, template_id, step_type, step_order, critical, enabled, parameters_json
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?::jsonb)
                    """,
                    UUID.randomUUID(),
                    templateId,
                    step.stepType().name(),
                    step.order(),
                    step.critical(),
                    step.enabled(),
                    toJson(step.parameters())
            );
        }
    }

    private ExecuteScriptTemplate mapTemplate(ResultSet rs, int rowNum) throws SQLException {
        UUID templateId = rs.getObject("id", UUID.class);
        return new ExecuteScriptTemplate(
                templateId,
                rs.getString("name"),
                rs.getString("description"),
                rs.getBoolean("active"),
                readTree(rs.getString("default_parameters_json")),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                rs.getObject("updated_by_user_id", UUID.class),
                templateSteps(templateId)
        );
    }

    private List<ExecuteScriptTemplateStep> templateSteps(UUID templateId) {
        return jdbcTemplate.query(
                """
                SELECT *
                FROM execute_script_template_steps
                WHERE template_id = ?
                ORDER BY step_order
                """,
                this::mapTemplateStep,
                templateId
        );
    }

    private ExecuteScriptTemplateStep mapTemplateStep(ResultSet rs, int rowNum) throws SQLException {
        return new ExecuteScriptTemplateStep(
                rs.getObject("id", UUID.class),
                rs.getObject("template_id", UUID.class),
                ExecuteScriptStepType.valueOf(rs.getString("step_type")),
                rs.getInt("step_order"),
                rs.getBoolean("critical"),
                rs.getBoolean("enabled"),
                readTree(rs.getString("parameters_json"))
        );
    }

    private ExecuteScriptRun mapRun(ResultSet rs, int rowNum) throws SQLException {
        UUID runId = rs.getObject("id", UUID.class);
        return new ExecuteScriptRun(
                runId,
                rs.getObject("template_id", UUID.class),
                rs.getString("template_name"),
                ExecuteScriptMode.valueOf(rs.getString("mode")),
                rs.getObject("business_date", LocalDate.class),
                ExecuteScriptRunStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("started_at").toInstant(),
                rs.getTimestamp("completed_at") == null ? null : rs.getTimestamp("completed_at").toInstant(),
                rs.getObject("requested_by_user_id", UUID.class),
                rs.getString("message"),
                readTree(rs.getString("input_json")),
                runSteps(runId)
        );
    }

    private List<ExecuteScriptRunStep> runSteps(UUID runId) {
        return jdbcTemplate.query(
                """
                SELECT *
                FROM execute_script_run_steps
                WHERE run_id = ?
                ORDER BY step_order
                """,
                this::mapRunStep,
                runId
        );
    }

    private ExecuteScriptRunStep mapRunStep(ResultSet rs, int rowNum) throws SQLException {
        return new ExecuteScriptRunStep(
                rs.getObject("id", UUID.class),
                rs.getObject("run_id", UUID.class),
                ExecuteScriptStepType.valueOf(rs.getString("step_type")),
                rs.getInt("step_order"),
                rs.getBoolean("critical"),
                ExecuteScriptStepStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("started_at") == null ? null : rs.getTimestamp("started_at").toInstant(),
                rs.getTimestamp("completed_at") == null ? null : rs.getTimestamp("completed_at").toInstant(),
                rs.getString("message"),
                readTree(rs.getString("output_json"))
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? java.util.Map.of() : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Could not serialize execute script data");
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
