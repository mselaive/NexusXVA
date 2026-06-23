package com.nexusxva.eod.infrastructure;

import com.nexusxva.eod.application.PortfolioEodStore;
import com.nexusxva.eod.domain.PortfolioEodSnapshot;
import com.nexusxva.eod.domain.PositionEodSnapshot;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcPortfolioEodStore implements PortfolioEodStore {

    private final JdbcTemplate jdbcTemplate;

    JdbcPortfolioEodStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PortfolioEodSnapshot create(PortfolioEodSnapshot snapshot) {
        jdbcTemplate.update(
                """
                INSERT INTO portfolio_eod_runs (
                    id, portfolio_id, business_date, base_currency,
                    total_market_value, total_trade_value, total_unrealized_pnl,
                    positions_without_execution_price, captured_at, source
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                snapshot.id(),
                snapshot.portfolioId(),
                snapshot.businessDate(),
                snapshot.baseCurrency(),
                snapshot.totalMarketValue(),
                snapshot.totalTradeValue(),
                snapshot.totalUnrealizedPnl(),
                snapshot.positionsWithoutExecutionPrice(),
                Timestamp.from(snapshot.capturedAt()),
                snapshot.source()
        );
        snapshot.positions().forEach(position -> jdbcTemplate.update(
                """
                INSERT INTO portfolio_position_eod_snapshots (
                    run_id, position_id, underlying_symbol, quantity,
                    unit_price, market_value, execution_price, trade_value, unrealized_pnl,
                    market_data_as_of, market_data_source, stale
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                snapshot.id(),
                position.positionId(),
                position.underlyingSymbol(),
                position.quantity(),
                position.unitPrice(),
                position.marketValue(),
                position.executionPrice(),
                position.tradeValue(),
                position.unrealizedPnl(),
                Timestamp.from(position.marketDataAsOf()),
                position.marketDataSource(),
                position.stale()
        ));
        return snapshot;
    }

    @Override
    public boolean exists(UUID portfolioId, LocalDate businessDate) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM portfolio_eod_runs WHERE portfolio_id = ? AND business_date = ?)",
                Boolean.class,
                portfolioId,
                businessDate
        );
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public Optional<PortfolioEodSnapshot> latest(UUID portfolioId) {
        List<PortfolioEodSnapshot> runs = jdbcTemplate.query(
                """
                SELECT *
                FROM portfolio_eod_runs
                WHERE portfolio_id = ?
                ORDER BY business_date DESC
                LIMIT 1
                """,
                this::mapRun,
                portfolioId
        );
        return runs.stream().findFirst().map(this::withPositions);
    }

    @Override
    public List<PortfolioEodSnapshot> history(UUID portfolioId, int limit) {
        return jdbcTemplate.query(
                """
                SELECT *
                FROM portfolio_eod_runs
                WHERE portfolio_id = ?
                ORDER BY business_date DESC
                LIMIT ?
                """,
                this::mapRun,
                portfolioId,
                limit
        ).stream().map(this::withPositions).toList();
    }

    private PortfolioEodSnapshot withPositions(PortfolioEodSnapshot run) {
        List<PositionEodSnapshot> positions = jdbcTemplate.query(
                """
                SELECT *
                FROM portfolio_position_eod_snapshots
                WHERE run_id = ?
                ORDER BY underlying_symbol, position_id
                """,
                this::mapPosition,
                run.id()
        );
        return new PortfolioEodSnapshot(
                run.id(),
                run.portfolioId(),
                run.businessDate(),
                run.baseCurrency(),
                run.totalMarketValue(),
                run.totalTradeValue(),
                run.totalUnrealizedPnl(),
                run.positionsWithoutExecutionPrice(),
                run.capturedAt(),
                run.source(),
                positions
        );
    }

    private PortfolioEodSnapshot mapRun(ResultSet rs, int rowNum) throws SQLException {
        return new PortfolioEodSnapshot(
                rs.getObject("id", UUID.class),
                rs.getObject("portfolio_id", UUID.class),
                rs.getObject("business_date", LocalDate.class),
                rs.getString("base_currency"),
                rs.getDouble("total_market_value"),
                rs.getDouble("total_trade_value"),
                rs.getDouble("total_unrealized_pnl"),
                rs.getInt("positions_without_execution_price"),
                rs.getTimestamp("captured_at").toInstant(),
                rs.getString("source"),
                List.of()
        );
    }

    private PositionEodSnapshot mapPosition(ResultSet rs, int rowNum) throws SQLException {
        return new PositionEodSnapshot(
                rs.getObject("position_id", UUID.class),
                rs.getString("underlying_symbol"),
                rs.getDouble("quantity"),
                rs.getDouble("unit_price"),
                rs.getDouble("market_value"),
                nullableDouble(rs, "execution_price"),
                nullableDouble(rs, "trade_value"),
                nullableDouble(rs, "unrealized_pnl"),
                rs.getTimestamp("market_data_as_of").toInstant(),
                rs.getString("market_data_source"),
                rs.getBoolean("stale")
        );
    }

    private Double nullableDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }
}
