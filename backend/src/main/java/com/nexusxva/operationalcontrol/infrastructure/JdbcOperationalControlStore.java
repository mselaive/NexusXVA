package com.nexusxva.operationalcontrol.infrastructure;

import com.nexusxva.operationalcontrol.application.OperationalControlStore;
import com.nexusxva.operationalcontrol.domain.OperationalControlSettings;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcOperationalControlStore implements OperationalControlStore {

    private final JdbcTemplate jdbcTemplate;

    JdbcOperationalControlStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public OperationalControlSettings settings() {
        return jdbcTemplate.queryForObject(
                """
                SELECT *
                FROM operational_control_settings
                WHERE id = 1
                """,
                this::mapSettings
        );
    }

    @Override
    public OperationalControlSettings save(OperationalControlSettings settings, UUID updatedByUserId) {
        jdbcTemplate.update(
                """
                UPDATE operational_control_settings
                SET timezone = ?,
                    business_days = ?,
                    trading_open_time = ?,
                    trading_close_time = ?,
                    enforce_operational_window = ?,
                    block_trade_bookings_outside_window = ?,
                    block_risk_runs_outside_window = ?,
                    eod_enabled = ?,
                    eod_run_time = ?,
                    eod_allow_stale_market_data = ?,
                    updated_at = ?,
                    updated_by_user_id = ?,
                    version = version + 1
                WHERE id = 1
                """,
                settings.timezone().getId(),
                encodeDays(settings.businessDays()),
                settings.tradingOpenTime(),
                settings.tradingCloseTime(),
                settings.enforceOperationalWindow(),
                settings.blockTradeBookingsOutsideWindow(),
                settings.blockRiskRunsOutsideWindow(),
                settings.eodEnabled(),
                settings.eodRunTime(),
                settings.eodAllowStaleMarketData(),
                Timestamp.from(Instant.now()),
                updatedByUserId
        );
        return settings();
    }

    @Override
    public boolean tryStartScheduledEod(UUID runId, LocalDate businessDate, Instant startedAt) {
        try {
            jdbcTemplate.update(
                    """
                    INSERT INTO eod_scheduler_runs (id, business_date, started_at, status, message)
                    VALUES (?, ?, ?, 'RUNNING', ?)
                    """,
                    runId,
                    businessDate,
                    Timestamp.from(startedAt),
                    "Scheduled EOD started"
            );
            return true;
        } catch (DuplicateKeyException exception) {
            return false;
        }
    }

    @Override
    public void completeScheduledEod(UUID runId, int captured, int skipped, int failed, String message) {
        jdbcTemplate.update(
                """
                UPDATE eod_scheduler_runs
                SET completed_at = ?,
                    status = ?,
                    captured = ?,
                    skipped = ?,
                    failed = ?,
                    message = ?
                WHERE id = ?
                """,
                Timestamp.from(Instant.now()),
                failed > 0 ? "FAILED" : "COMPLETED",
                captured,
                skipped,
                failed,
                truncate(message),
                runId
        );
    }

    @Override
    public void failScheduledEod(UUID runId, String message) {
        jdbcTemplate.update(
                """
                UPDATE eod_scheduler_runs
                SET completed_at = ?,
                    status = 'FAILED',
                    message = ?
                WHERE id = ?
                """,
                Timestamp.from(Instant.now()),
                truncate(message),
                runId
        );
    }

    @Override
    public Optional<LocalDate> latestScheduledEodBusinessDate() {
        return jdbcTemplate.query(
                """
                SELECT business_date
                FROM eod_scheduler_runs
                ORDER BY started_at DESC
                LIMIT 1
                """,
                (rs, rowNum) -> rs.getObject("business_date", LocalDate.class)
        ).stream().findFirst();
    }

    private OperationalControlSettings mapSettings(ResultSet rs, int rowNum) throws SQLException {
        return new OperationalControlSettings(
                ZoneId.of(rs.getString("timezone")),
                decodeDays(rs.getString("business_days")),
                rs.getObject("trading_open_time", LocalTime.class),
                rs.getObject("trading_close_time", LocalTime.class),
                rs.getBoolean("block_trade_bookings_outside_window"),
                rs.getBoolean("block_risk_runs_outside_window"),
                rs.getBoolean("eod_enabled"),
                rs.getObject("eod_run_time", LocalTime.class),
                rs.getBoolean("eod_allow_stale_market_data"),
                rs.getTimestamp("updated_at").toInstant(),
                rs.getObject("updated_by_user_id", UUID.class),
                rs.getLong("version")
        );
    }

    private String encodeDays(Set<DayOfWeek> days) {
        return days.stream().sorted().map(DayOfWeek::name).reduce((left, right) -> left + "," + right).orElse("");
    }

    private Set<DayOfWeek> decodeDays(String value) {
        EnumSet<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        if (value != null && !value.isBlank()) {
            Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(part -> !part.isBlank())
                    .map(DayOfWeek::valueOf)
                    .forEach(days::add);
        }
        return days;
    }

    private String truncate(String message) {
        if (message == null || message.isBlank()) {
            return null;
        }
        return message.length() <= 500 ? message : message.substring(0, 500);
    }
}
