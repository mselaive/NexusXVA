package com.nexusxva.tradinglimits.infrastructure;

import com.nexusxva.shared.error.ConflictException;
import com.nexusxva.shared.error.ResourceNotFoundException;
import com.nexusxva.tradebooking.domain.BookingActor;
import com.nexusxva.tradinglimits.application.TradingLimitStore;
import com.nexusxva.tradinglimits.application.TradingLimitUserPage;
import com.nexusxva.tradinglimits.application.UpdateTradingLimitCommand;
import com.nexusxva.tradinglimits.domain.TradingLimitPolicy;
import com.nexusxva.tradinglimits.domain.TradingLimitSnapshot;
import com.nexusxva.tradinglimits.domain.TradingLimitUsage;
import com.nexusxva.tradinglimits.domain.TradingLimitWindows;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcTradingLimitStore implements TradingLimitStore {

    private static final String POLICY_COLUMNS = """
            user_id, max_trades_per_hour, max_trades_per_day,
            max_notional_per_hour, max_notional_per_day, notional_currency,
            active, created_at, updated_at, updated_by_user_id,
            updated_by_username, updated_by_display_name, version
            """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcTradingLimitStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<TradingLimitPolicy> findPolicy(UUID userId) {
        return queryPolicy("SELECT " + POLICY_COLUMNS + " FROM trading_limit_policies WHERE user_id = ?", userId);
    }

    @Override
    public Optional<TradingLimitPolicy> findPolicyForUpdate(UUID userId) {
        return queryPolicy(
                "SELECT " + POLICY_COLUMNS + " FROM trading_limit_policies WHERE user_id = ? FOR UPDATE",
                userId
        );
    }

    @Override
    public TradingLimitPolicy savePolicy(
            UUID userId,
            UpdateTradingLimitCommand command,
            BookingActor updatedBy
    ) {
        Optional<TradingLimitPolicy> current = findPolicyForUpdate(userId);
        Instant now = Instant.now();
        if (current.isEmpty()) {
            if (command.version() != null && command.version() != 0) {
                throw new ConflictException("Trading limit policy version conflict");
            }
            jdbcTemplate.update(
                    """
                    INSERT INTO trading_limit_policies (
                        user_id, max_trades_per_hour, max_trades_per_day,
                        max_notional_per_hour, max_notional_per_day, notional_currency,
                        active, created_at, updated_at, updated_by_user_id,
                        updated_by_username, updated_by_display_name, version
                    )
                    VALUES (?, ?, ?, ?, ?, 'USD', ?, ?, ?, ?, ?, ?, 0)
                    """,
                    userId,
                    command.maxTradesPerHour(),
                    command.maxTradesPerDay(),
                    command.maxNotionalPerHour(),
                    command.maxNotionalPerDay(),
                    command.active(),
                    Timestamp.from(now),
                    Timestamp.from(now),
                    updatedBy.userId(),
                    updatedBy.username(),
                    updatedBy.displayName()
            );
        } else {
            long expectedVersion = command.version() == null ? -1 : command.version();
            int updated = jdbcTemplate.update(
                    """
                    UPDATE trading_limit_policies
                    SET max_trades_per_hour = ?,
                        max_trades_per_day = ?,
                        max_notional_per_hour = ?,
                        max_notional_per_day = ?,
                        active = ?,
                        updated_at = ?,
                        updated_by_user_id = ?,
                        updated_by_username = ?,
                        updated_by_display_name = ?,
                        version = version + 1
                    WHERE user_id = ? AND version = ?
                    """,
                    command.maxTradesPerHour(),
                    command.maxTradesPerDay(),
                    command.maxNotionalPerHour(),
                    command.maxNotionalPerDay(),
                    command.active(),
                    Timestamp.from(now),
                    updatedBy.userId(),
                    updatedBy.username(),
                    updatedBy.displayName(),
                    userId,
                    expectedVersion
            );
            if (updated == 0) {
                throw new ConflictException("Trading limit policy version conflict");
            }
        }
        return findPolicy(userId).orElseThrow();
    }

    @Override
    public TradingLimitUsage usage(UUID userId, TradingLimitWindows windows) {
        UsageRow hour = usageRow(userId, windows.hourStartsAt(), windows.hourEndsAt());
        UsageRow day = usageRow(userId, windows.dayStartsAt(), windows.dayEndsAt());
        return new TradingLimitUsage(
                hour.trades(),
                day.trades(),
                hour.notional(),
                day.notional(),
                windows.hourEndsAt(),
                windows.dayEndsAt()
        );
    }

    @Override
    public TradingLimitSnapshot snapshot(UUID userId, TradingLimitWindows windows) {
        UserRow user = findFoUser(userId);
        TradingLimitPolicy policy = findPolicy(userId).orElse(null);
        String status = policy == null || !policy.active() ? "UNLIMITED" : "ACTIVE";
        if (policy != null && !policy.active()) {
            status = "DISABLED";
        }
        return new TradingLimitSnapshot(
                user.id(),
                user.username(),
                user.displayName(),
                status,
                policy,
                usage(userId, windows)
        );
    }

    @Override
    public TradingLimitUserPage searchFoUsers(
            String query,
            int page,
            int size,
            TradingLimitWindows windows
    ) {
        String pattern = query == null || query.isBlank() ? "%" : "%" + query.toLowerCase() + "%";
        Long total = jdbcTemplate.queryForObject(
                """
                SELECT count(DISTINCT u.id)
                FROM auth_user_accounts u
                JOIN auth_user_group_memberships m ON m.user_id = u.id
                JOIN auth_groups g ON g.id = m.group_id
                WHERE u.active = TRUE
                  AND g.code = 'FO'
                  AND (lower(u.username) LIKE ? OR lower(u.display_name) LIKE ?)
                """,
                Long.class,
                pattern,
                pattern
        );
        List<UUID> userIds = jdbcTemplate.query(
                """
                SELECT DISTINCT u.id, u.display_name
                FROM auth_user_accounts u
                JOIN auth_user_group_memberships m ON m.user_id = u.id
                JOIN auth_groups g ON g.id = m.group_id
                WHERE u.active = TRUE
                  AND g.code = 'FO'
                  AND (lower(u.username) LIKE ? OR lower(u.display_name) LIKE ?)
                ORDER BY u.display_name, u.id
                LIMIT ? OFFSET ?
                """,
                (rs, rowNum) -> rs.getObject("id", UUID.class),
                pattern,
                pattern,
                size,
                page * size
        );
        List<TradingLimitSnapshot> items = userIds.stream()
                .map(userId -> snapshot(userId, windows))
                .toList();
        long totalElements = total == null ? 0 : total;
        int totalPages = (int) Math.ceil(totalElements / (double) size);
        return new TradingLimitUserPage(items, page, size, totalElements, totalPages);
    }

    @Override
    public boolean isActiveFoUser(UUID userId) {
        Boolean exists = jdbcTemplate.queryForObject(
                """
                SELECT EXISTS (
                    SELECT 1
                    FROM auth_user_accounts u
                    JOIN auth_user_group_memberships m ON m.user_id = u.id
                    JOIN auth_groups g ON g.id = m.group_id
                    WHERE u.id = ? AND u.active = TRUE AND g.code = 'FO'
                )
                """,
                Boolean.class,
                userId
        );
        return Boolean.TRUE.equals(exists);
    }

    private Optional<TradingLimitPolicy> queryPolicy(String sql, UUID userId) {
        List<TradingLimitPolicy> policies = jdbcTemplate.query(sql, this::policy, userId);
        return policies.stream().findFirst();
    }

    private TradingLimitPolicy policy(ResultSet rs, int rowNum) throws SQLException {
        return new TradingLimitPolicy(
                rs.getObject("user_id", UUID.class),
                (Integer) rs.getObject("max_trades_per_hour"),
                (Integer) rs.getObject("max_trades_per_day"),
                rs.getBigDecimal("max_notional_per_hour"),
                rs.getBigDecimal("max_notional_per_day"),
                rs.getString("notional_currency"),
                rs.getBoolean("active"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                rs.getObject("updated_by_user_id", UUID.class),
                rs.getString("updated_by_username"),
                rs.getString("updated_by_display_name"),
                rs.getLong("version")
        );
    }

    private UsageRow usageRow(UUID userId, Instant startsAt, Instant endsAt) {
        return jdbcTemplate.queryForObject(
                """
                SELECT count(*) AS trades,
                       COALESCE(sum(COALESCE(booking_notional, abs(quantity) * strike)), 0) AS notional
                FROM trade_booking_requests
                WHERE submitted_by_user_id = ?
                  AND submitted_at >= ?
                  AND submitted_at < ?
                """,
                (rs, rowNum) -> new UsageRow(rs.getLong("trades"), rs.getBigDecimal("notional")),
                userId,
                Timestamp.from(startsAt),
                Timestamp.from(endsAt)
        );
    }

    private UserRow findFoUser(UUID userId) {
        List<UserRow> users = jdbcTemplate.query(
                """
                SELECT DISTINCT u.id, u.username, u.display_name
                FROM auth_user_accounts u
                JOIN auth_user_group_memberships m ON m.user_id = u.id
                JOIN auth_groups g ON g.id = m.group_id
                WHERE u.id = ? AND u.active = TRUE AND g.code = 'FO'
                """,
                (rs, rowNum) -> new UserRow(
                        rs.getObject("id", UUID.class),
                        rs.getString("username"),
                        rs.getString("display_name")
                ),
                userId
        );
        return users.stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("FO user not found"));
    }

    private record UsageRow(long trades, BigDecimal notional) {
    }

    private record UserRow(UUID id, String username, String displayName) {
    }
}
