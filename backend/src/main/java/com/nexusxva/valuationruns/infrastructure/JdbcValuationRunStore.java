package com.nexusxva.valuationruns.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusxva.valuationruns.application.ValuationRunSearchCriteria;
import com.nexusxva.valuationruns.application.ValuationRunStore;
import com.nexusxva.valuationruns.domain.ValuationRun;
import com.nexusxva.valuationruns.domain.ValuationRunStatus;
import com.nexusxva.valuationruns.domain.ValuationRunType;

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
class JdbcValuationRunStore implements ValuationRunStore {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    JdbcValuationRunStore(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public ValuationRun save(ValuationRun run) {
        jdbcTemplate.update(
                """
                INSERT INTO valuation_runs (
                    id, portfolio_id, portfolio_name_snapshot, run_type, model, valuation_date, status,
                    requested_by_user_id, requested_by_username, requested_by_display_name, active_group_code,
                    input_json, result_json, summary_json, error_message, created_at
                )
                VALUES (?, ?, (SELECT name FROM portfolios WHERE id = ?), ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), CAST(? AS jsonb), CAST(? AS jsonb), ?, ?)
                """,
                run.id(),
                run.portfolioId(),
                run.portfolioId(),
                run.runType().name(),
                run.model(),
                run.valuationDate(),
                run.status().name(),
                run.requestedByUserId(),
                run.requestedByUsername(),
                run.requestedByDisplayName(),
                run.activeGroupCode(),
                json(run.input()),
                json(run.result()),
                json(run.summary()),
                run.errorMessage(),
                Timestamp.from(run.createdAt())
        );
        return find(run.id()).orElse(run);
    }

    @Override
    public Optional<ValuationRun> find(UUID runId) {
        List<ValuationRun> runs = jdbcTemplate.query(
                """
                SELECT vr.*, COALESCE(p.name, vr.portfolio_name_snapshot, 'Unknown portfolio') AS portfolio_name
                FROM valuation_runs vr
                LEFT JOIN portfolios p ON p.id = vr.portfolio_id
                WHERE vr.id = ?
                """,
                this::mapRun,
                runId
        );
        return runs.stream().findFirst();
    }

    @Override
    public List<ValuationRun> search(ValuationRunSearchCriteria criteria) {
        StringBuilder sql = new StringBuilder("""
                SELECT vr.*, COALESCE(p.name, vr.portfolio_name_snapshot, 'Unknown portfolio') AS portfolio_name
                FROM valuation_runs vr
                LEFT JOIN portfolios p ON p.id = vr.portfolio_id
                WHERE 1 = 1
                """);
        List<Object> params = new ArrayList<>();

        if (criteria.runType() != null) {
            sql.append(" AND vr.run_type = ?");
            params.add(criteria.runType().name());
        }
        if (criteria.status() != null) {
            sql.append(" AND vr.status = ?");
            params.add(criteria.status().name());
        }
        if (criteria.portfolioId() != null) {
            sql.append(" AND vr.portfolio_id = ?");
            params.add(criteria.portfolioId());
        }
        if (criteria.visiblePortfolioIds() != null) {
            if (criteria.visiblePortfolioIds().isEmpty()) {
                return List.of();
            }
            sql.append(" AND vr.portfolio_id IN (");
            sql.append("?,".repeat(criteria.visiblePortfolioIds().size()));
            sql.setLength(sql.length() - 1);
            sql.append(")");
            params.addAll(criteria.visiblePortfolioIds());
        }
        sql.append(" ORDER BY vr.created_at DESC LIMIT ?");
        params.add(criteria.limit());

        return jdbcTemplate.query(sql.toString(), this::mapRun, params.toArray());
    }

    private ValuationRun mapRun(ResultSet rs, int rowNum) throws SQLException {
        return new ValuationRun(
                rs.getObject("id", UUID.class),
                rs.getObject("portfolio_id", UUID.class),
                rs.getString("portfolio_name"),
                ValuationRunType.valueOf(rs.getString("run_type")),
                rs.getString("model"),
                rs.getObject("valuation_date", LocalDate.class),
                ValuationRunStatus.valueOf(rs.getString("status")),
                rs.getObject("requested_by_user_id", UUID.class),
                rs.getString("requested_by_username"),
                rs.getString("requested_by_display_name"),
                rs.getString("active_group_code"),
                parseJson(rs.getString("input_json")),
                parseJson(rs.getString("result_json")),
                parseJson(rs.getString("summary_json")),
                rs.getString("error_message"),
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
            throw new IllegalStateException("Stored valuation run JSON is unreadable", exception);
        }
    }

    private String json(JsonNode node) {
        return node == null ? null : node.toString();
    }
}
