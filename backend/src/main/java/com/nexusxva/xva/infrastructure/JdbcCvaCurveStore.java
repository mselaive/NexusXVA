package com.nexusxva.xva.infrastructure;

import com.nexusxva.xva.application.CvaCurveStore;
import com.nexusxva.xva.application.SaveCreditCurveCommand;
import com.nexusxva.xva.application.SaveDiscountCurveCommand;
import com.nexusxva.xva.domain.CreditCurve;
import com.nexusxva.xva.domain.CreditCurveType;
import com.nexusxva.xva.domain.CurveLifecycleStatus;
import com.nexusxva.xva.domain.DiscountCurve;
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
class JdbcCvaCurveStore implements CvaCurveStore {

    private final JdbcTemplate jdbcTemplate;

    JdbcCvaCurveStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public CreditCurve createCreditCurve(SaveCreditCurveCommand command) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        int version = nextCreditCurveVersion(command.counterpartyId(), command.name().trim());
        jdbcTemplate.update(
                """
                INSERT INTO credit_curves (
                    id, counterparty_id, name, curve_type, version, status, source, active,
                    created_at, updated_at, submitted_at
                )
                VALUES (?, ?, ?, ?, ?, 'DRAFT', ?, FALSE, ?, ?, ?)
                """,
                id,
                command.counterpartyId(),
                command.name().trim(),
                command.curveType().name(),
                version,
                command.source().name(),
                Timestamp.from(now),
                Timestamp.from(now),
                Timestamp.from(now)
        );
        replaceCreditPoints(id, command.points());
        return findCreditCurve(id).orElseThrow();
    }

    @Override
    public CreditCurve updateCreditCurve(UUID curveId, SaveCreditCurveCommand command) {
        jdbcTemplate.update(
                """
                UPDATE credit_curves
                SET counterparty_id = ?, name = ?, curve_type = ?, active = FALSE, updated_at = ?
                WHERE id = ?
                """,
                command.counterpartyId(),
                command.name().trim(),
                command.curveType().name(),
                Timestamp.from(Instant.now()),
                curveId
        );
        replaceCreditPoints(curveId, command.points());
        return findCreditCurve(curveId).orElseThrow();
    }

    @Override
    public CreditCurve approveCreditCurve(UUID curveId, UUID approvedByUserId) {
        CreditCurve curve = findCreditCurve(curveId).orElseThrow();
        if (curve.status() != CurveLifecycleStatus.DRAFT) {
            throw new IllegalArgumentException("Only draft credit curves can be approved");
        }
        Instant now = Instant.now();
        jdbcTemplate.update(
                """
                UPDATE credit_curves
                SET active = FALSE, status = 'SUPERSEDED', updated_at = ?
                WHERE counterparty_id = ? AND name = ? AND active = TRUE AND status = 'APPROVED'
                """,
                Timestamp.from(now),
                curve.counterpartyId(),
                curve.name()
        );
        jdbcTemplate.update(
                """
                UPDATE credit_curves
                SET active = TRUE, status = 'APPROVED', approved_at = ?, approved_by_user_id = ?, updated_at = ?
                WHERE id = ?
                """,
                Timestamp.from(now),
                approvedByUserId,
                Timestamp.from(now),
                curveId
        );
        return findCreditCurve(curveId).orElseThrow();
    }

    @Override
    public CreditCurve rejectCreditCurve(UUID curveId, String reason) {
        CreditCurve curve = findCreditCurve(curveId).orElseThrow();
        if (curve.status() != CurveLifecycleStatus.DRAFT) {
            throw new IllegalArgumentException("Only draft credit curves can be rejected");
        }
        jdbcTemplate.update(
                """
                UPDATE credit_curves
                SET active = FALSE, status = 'REJECTED', rejection_reason = ?, updated_at = ?
                WHERE id = ?
                """,
                reason,
                Timestamp.from(Instant.now()),
                curveId
        );
        return findCreditCurve(curveId).orElseThrow();
    }

    @Override
    public List<CreditCurve> listCreditCurves(UUID counterpartyId, boolean includeInactive) {
        StringBuilder sql = new StringBuilder(creditCurveSql("WHERE 1 = 1"));
        java.util.ArrayList<Object> params = new java.util.ArrayList<>();
        if (counterpartyId != null) {
            sql.append(" AND cc.counterparty_id = ?");
            params.add(counterpartyId);
        }
        if (!includeInactive) {
            sql.append(" AND cc.active = TRUE AND cc.status = 'APPROVED' AND cp.active = TRUE");
        }
        sql.append(" ORDER BY cp.name, cc.name, cc.version DESC");
        return jdbcTemplate.query(sql.toString(), this::mapCreditCurve, params.toArray());
    }

    @Override
    public Optional<CreditCurve> findCreditCurve(UUID curveId) {
        return jdbcTemplate.query(creditCurveSql("WHERE cc.id = ?"), this::mapCreditCurve, curveId)
                .stream()
                .findFirst();
    }

    @Override
    public DiscountCurve createDiscountCurve(SaveDiscountCurveCommand command) {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        int version = nextDiscountCurveVersion(command.name().trim(), command.currency());
        jdbcTemplate.update(
                """
                INSERT INTO discount_curves (
                    id, name, currency, version, status, source, active,
                    created_at, updated_at, submitted_at
                )
                VALUES (?, ?, ?, ?, 'DRAFT', ?, FALSE, ?, ?, ?)
                """,
                id,
                command.name().trim(),
                command.currency(),
                version,
                command.source().name(),
                Timestamp.from(now),
                Timestamp.from(now),
                Timestamp.from(now)
        );
        replaceDiscountPoints(id, command.points());
        return findDiscountCurve(id).orElseThrow();
    }

    @Override
    public DiscountCurve updateDiscountCurve(UUID curveId, SaveDiscountCurveCommand command) {
        jdbcTemplate.update(
                """
                UPDATE discount_curves
                SET name = ?, currency = ?, active = FALSE, updated_at = ?
                WHERE id = ?
                """,
                command.name().trim(),
                command.currency(),
                Timestamp.from(Instant.now()),
                curveId
        );
        replaceDiscountPoints(curveId, command.points());
        return findDiscountCurve(curveId).orElseThrow();
    }

    @Override
    public DiscountCurve approveDiscountCurve(UUID curveId, UUID approvedByUserId) {
        DiscountCurve curve = findDiscountCurve(curveId).orElseThrow();
        if (curve.status() != CurveLifecycleStatus.DRAFT) {
            throw new IllegalArgumentException("Only draft discount curves can be approved");
        }
        Instant now = Instant.now();
        jdbcTemplate.update(
                """
                UPDATE discount_curves
                SET active = FALSE, status = 'SUPERSEDED', updated_at = ?
                WHERE name = ? AND currency = ? AND active = TRUE AND status = 'APPROVED'
                """,
                Timestamp.from(now),
                curve.name(),
                curve.currency()
        );
        jdbcTemplate.update(
                """
                UPDATE discount_curves
                SET active = TRUE, status = 'APPROVED', approved_at = ?, approved_by_user_id = ?, updated_at = ?
                WHERE id = ?
                """,
                Timestamp.from(now),
                approvedByUserId,
                Timestamp.from(now),
                curveId
        );
        return findDiscountCurve(curveId).orElseThrow();
    }

    @Override
    public DiscountCurve rejectDiscountCurve(UUID curveId, String reason) {
        DiscountCurve curve = findDiscountCurve(curveId).orElseThrow();
        if (curve.status() != CurveLifecycleStatus.DRAFT) {
            throw new IllegalArgumentException("Only draft discount curves can be rejected");
        }
        jdbcTemplate.update(
                """
                UPDATE discount_curves
                SET active = FALSE, status = 'REJECTED', rejection_reason = ?, updated_at = ?
                WHERE id = ?
                """,
                reason,
                Timestamp.from(Instant.now()),
                curveId
        );
        return findDiscountCurve(curveId).orElseThrow();
    }

    @Override
    public List<DiscountCurve> listDiscountCurves(String currency, boolean includeInactive) {
        StringBuilder sql = new StringBuilder("SELECT * FROM discount_curves dc WHERE 1 = 1");
        java.util.ArrayList<Object> params = new java.util.ArrayList<>();
        if (currency != null && !currency.isBlank()) {
            sql.append(" AND dc.currency = ?");
            params.add(currency.trim().toUpperCase());
        }
        if (!includeInactive) {
            sql.append(" AND dc.active = TRUE AND dc.status = 'APPROVED'");
        }
        sql.append(" ORDER BY dc.currency, dc.name, dc.version DESC");
        return jdbcTemplate.query(sql.toString(), this::mapDiscountCurve, params.toArray());
    }

    @Override
    public Optional<DiscountCurve> findDiscountCurve(UUID curveId) {
        return jdbcTemplate.query("SELECT * FROM discount_curves dc WHERE dc.id = ?", this::mapDiscountCurve, curveId)
                .stream()
                .findFirst();
    }

    private String creditCurveSql(String whereClause) {
        return """
                SELECT cc.*, cp.name AS counterparty_name
                FROM credit_curves cc
                JOIN counterparties cp ON cp.id = cc.counterparty_id
                %s
                """.formatted(whereClause);
    }

    private CreditCurve mapCreditCurve(ResultSet rs, int rowNum) throws SQLException {
        UUID id = rs.getObject("id", UUID.class);
        return new CreditCurve(
                id,
                rs.getObject("counterparty_id", UUID.class),
                rs.getString("counterparty_name"),
                rs.getString("name"),
                CreditCurveType.valueOf(rs.getString("curve_type")),
                rs.getInt("version"),
                CurveLifecycleStatus.valueOf(rs.getString("status")),
                rs.getString("source"),
                rs.getBoolean("active"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                nullableInstant(rs, "submitted_at"),
                nullableInstant(rs, "approved_at"),
                rs.getObject("approved_by_user_id", UUID.class),
                rs.getString("rejection_reason"),
                creditPoints(id)
        );
    }

    private DiscountCurve mapDiscountCurve(ResultSet rs, int rowNum) throws SQLException {
        UUID id = rs.getObject("id", UUID.class);
        return new DiscountCurve(
                id,
                rs.getString("name"),
                rs.getString("currency"),
                rs.getInt("version"),
                CurveLifecycleStatus.valueOf(rs.getString("status")),
                rs.getString("source"),
                rs.getBoolean("active"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                nullableInstant(rs, "submitted_at"),
                nullableInstant(rs, "approved_at"),
                rs.getObject("approved_by_user_id", UUID.class),
                rs.getString("rejection_reason"),
                discountPoints(id)
        );
    }

    private int nextCreditCurveVersion(UUID counterpartyId, String name) {
        Integer version = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(version), 0) + 1 FROM credit_curves WHERE counterparty_id = ? AND name = ?",
                Integer.class,
                counterpartyId,
                name
        );
        return version == null ? 1 : version;
    }

    private int nextDiscountCurveVersion(String name, String currency) {
        Integer version = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(version), 0) + 1 FROM discount_curves WHERE name = ? AND currency = ?",
                Integer.class,
                name,
                currency
        );
        return version == null ? 1 : version;
    }

    private List<CreditCurve.Point> creditPoints(UUID curveId) {
        return jdbcTemplate.query(
                """
                SELECT point_date, survival_probability, cumulative_default_probability
                FROM credit_curve_points
                WHERE credit_curve_id = ?
                ORDER BY point_date
                """,
                (rs, rowNum) -> new CreditCurve.Point(
                        rs.getObject("point_date", LocalDate.class),
                        nullableDouble(rs, "survival_probability"),
                        nullableDouble(rs, "cumulative_default_probability")
                ),
                curveId
        );
    }

    private List<DiscountCurve.Point> discountPoints(UUID curveId) {
        return jdbcTemplate.query(
                """
                SELECT point_date, discount_factor
                FROM discount_curve_points
                WHERE discount_curve_id = ?
                ORDER BY point_date
                """,
                (rs, rowNum) -> new DiscountCurve.Point(
                        rs.getObject("point_date", LocalDate.class),
                        rs.getDouble("discount_factor")
                ),
                curveId
        );
    }

    private void replaceCreditPoints(UUID curveId, List<CreditCurve.Point> points) {
        jdbcTemplate.update("DELETE FROM credit_curve_points WHERE credit_curve_id = ?", curveId);
        for (CreditCurve.Point point : points) {
            jdbcTemplate.update(
                    """
                    INSERT INTO credit_curve_points (
                        credit_curve_id, point_date, survival_probability, cumulative_default_probability
                    )
                    VALUES (?, ?, ?, ?)
                    """,
                    curveId,
                    point.date(),
                    point.survivalProbability(),
                    point.cumulativeDefaultProbability()
            );
        }
    }

    private void replaceDiscountPoints(UUID curveId, List<DiscountCurve.Point> points) {
        jdbcTemplate.update("DELETE FROM discount_curve_points WHERE discount_curve_id = ?", curveId);
        for (DiscountCurve.Point point : points) {
            jdbcTemplate.update(
                    """
                    INSERT INTO discount_curve_points (discount_curve_id, point_date, discount_factor)
                    VALUES (?, ?, ?)
                    """,
                    curveId,
                    point.date(),
                    point.discountFactor()
            );
        }
    }

    private Double nullableDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    private Instant nullableInstant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
