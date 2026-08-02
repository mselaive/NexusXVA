package com.nexusxva.riskcockpit.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusxva.riskcockpit.application.RiskPackStore;
import com.nexusxva.riskcockpit.domain.*;
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
class JdbcRiskPackStore implements RiskPackStore {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    JdbcRiskPackStore(JdbcTemplate jdbc, ObjectMapper mapper) { this.jdbc = jdbc; this.mapper = mapper; }

    @Override public RiskPackRun create(UUID runId, UUID portfolioId, LocalDate valuationDate, UUID userId, String username,
                                        String group, Instant portfolioUpdatedAt, JsonNode configuration, Instant now) {
        jdbc.update("""
                INSERT INTO risk_pack_runs (id, portfolio_id, valuation_date, status, requested_by_user_id,
                  requested_by_username, requested_by_group, portfolio_updated_at, configuration_json, queued_at)
                VALUES (?, ?, ?, 'QUEUED', ?, ?, ?, ?, ?::jsonb, ?)
                """, runId, portfolioId, valuationDate, userId, username, group, Timestamp.from(portfolioUpdatedAt),
                json(configuration), Timestamp.from(now));
        for (RiskPackComponentType type : RiskPackComponentType.values()) {
            jdbc.update("INSERT INTO risk_pack_run_components (id, run_id, component_type, status) VALUES (?, ?, ?, 'PENDING')",
                    UUID.randomUUID(), runId, type.name());
        }
        return find(runId).orElseThrow();
    }
    @Override public Optional<RiskPackRun> find(UUID runId) { return jdbc.query("SELECT * FROM risk_pack_runs WHERE id=?", this::mapRun, runId).stream().findFirst(); }
    @Override public Optional<RiskPackRun> latest(UUID portfolioId) { return jdbc.query("SELECT * FROM risk_pack_runs WHERE portfolio_id=? ORDER BY queued_at DESC LIMIT 1", this::mapRun, portfolioId).stream().findFirst(); }
    @Override public List<RiskPackRun> recent(UUID portfolioId, int limit) { return jdbc.query("SELECT * FROM risk_pack_runs WHERE portfolio_id=? ORDER BY queued_at DESC LIMIT ?", this::mapRun, portfolioId, Math.max(1, Math.min(limit, 100))); }
    @Override public boolean hasActiveRun(UUID portfolioId) { return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM risk_pack_runs WHERE portfolio_id=? AND status IN ('QUEUED','RUNNING'))", Boolean.class, portfolioId)); }
    @Override public void markRunRunning(UUID runId, Instant now) { jdbc.update("UPDATE risk_pack_runs SET status='RUNNING', started_at=? WHERE id=?", Timestamp.from(now), runId); }
    @Override public void completeRun(UUID runId, RiskPackRunStatus status, Instant asOf, String error, Instant now) {
        jdbc.update("UPDATE risk_pack_runs SET status=?, market_data_as_of=?, error_message=?, completed_at=? WHERE id=?",
                status.name(), timestamp(asOf), truncate(error), Timestamp.from(now), runId);
    }
    @Override public void markComponentRunning(UUID runId, RiskPackComponentType type, Instant now) {
        jdbc.update("UPDATE risk_pack_run_components SET status='RUNNING', started_at=? WHERE run_id=? AND component_type=?", Timestamp.from(now), runId, type.name());
    }
    @Override public void completeComponent(UUID runId, RiskPackComponentType type, RiskPackComponentStatus status, JsonNode output, String error, Instant now) {
        jdbc.update("""
                UPDATE risk_pack_run_components SET status=?, completed_at=?, output_json=?::jsonb, error_message=?,
                  duration_ms=EXTRACT(EPOCH FROM (? - started_at))*1000 WHERE run_id=? AND component_type=?
                """, status.name(), Timestamp.from(now), output == null ? null : json(output), truncate(error), Timestamp.from(now), runId, type.name());
    }
    @Override public int failAbandonedRuns(Instant now) {
        jdbc.update("UPDATE risk_pack_run_components SET status='FAILED', completed_at=?, error_message='Backend restarted during execution' WHERE status='RUNNING'", Timestamp.from(now));
        jdbc.update("UPDATE risk_pack_run_components SET status='SKIPPED', completed_at=?, error_message='Run abandoned after backend restart' WHERE status='PENDING' AND run_id IN (SELECT id FROM risk_pack_runs WHERE status IN ('QUEUED','RUNNING'))", Timestamp.from(now));
        return jdbc.update("UPDATE risk_pack_runs SET status='FAILED', completed_at=?, error_message='Backend restarted during execution' WHERE status IN ('QUEUED','RUNNING')", Timestamp.from(now));
    }
    private RiskPackRun mapRun(ResultSet rs, int row) throws SQLException {
        UUID id=rs.getObject("id", UUID.class);
        return new RiskPackRun(id, rs.getObject("portfolio_id", UUID.class), rs.getObject("valuation_date", LocalDate.class),
                RiskPackRunStatus.valueOf(rs.getString("status")), rs.getObject("requested_by_user_id", UUID.class),
                rs.getString("requested_by_username"), rs.getString("requested_by_group"), rs.getTimestamp("portfolio_updated_at").toInstant(),
                instant(rs,"market_data_as_of"), tree(rs.getString("configuration_json")), rs.getTimestamp("queued_at").toInstant(),
                instant(rs,"started_at"), instant(rs,"completed_at"), rs.getString("error_message"), components(id));
    }
    private List<RiskPackComponent> components(UUID runId) { return jdbc.query("SELECT * FROM risk_pack_run_components WHERE run_id=? ORDER BY CASE component_type WHEN 'PRICING' THEN 1 WHEN 'STRESS' THEN 2 WHEN 'VAR' THEN 3 WHEN 'EXPOSURE' THEN 4 ELSE 5 END", this::mapComponent, runId); }
    private RiskPackComponent mapComponent(ResultSet rs,int row) throws SQLException { return new RiskPackComponent(rs.getObject("id",UUID.class), RiskPackComponentType.valueOf(rs.getString("component_type")), RiskPackComponentStatus.valueOf(rs.getString("status")), instant(rs,"started_at"), instant(rs,"completed_at"), (Long)rs.getObject("duration_ms"), tree(rs.getString("output_json")), rs.getString("error_message")); }
    private Instant instant(ResultSet rs,String column) throws SQLException { Timestamp value=rs.getTimestamp(column); return value==null?null:value.toInstant(); }
    private Timestamp timestamp(Instant value){ return value==null?null:Timestamp.from(value); }
    private String json(JsonNode value){ try{return mapper.writeValueAsString(value);}catch(JsonProcessingException e){throw new IllegalArgumentException("Unable to store risk pack output",e);} }
    private JsonNode tree(String value){ try{return value==null?null:mapper.readTree(value);}catch(JsonProcessingException e){throw new IllegalStateException("Stored risk pack JSON is invalid",e);} }
    private String truncate(String value){ return value==null?null:value.substring(0,Math.min(500,value.length())); }
}
