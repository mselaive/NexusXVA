package com.nexusxva.auth.infrastructure;

import com.nexusxva.auth.domain.AuthSession;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcUserAccessStore {

    private final JdbcTemplate jdbcTemplate;

    public JdbcUserAccessStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isFeatureAllowed(UUID userId, String permissionCode) {
        Boolean enabled = jdbcTemplate.query(
                """
                SELECT enabled
                FROM auth_user_feature_permission_overrides
                WHERE user_id = ? AND permission_code = ?
                """,
                rs -> rs.next() ? rs.getBoolean("enabled") : null,
                userId,
                permissionCode
        );
        return enabled == null || enabled;
    }

    public boolean hasAllPortfolioAccess(UUID userId) {
        String mode = jdbcTemplate.queryForObject(
                "SELECT portfolio_access_mode FROM auth_user_accounts WHERE id = ?",
                String.class,
                userId
        );
        return !"SELECTED".equals(mode);
    }

    public boolean canAccessPortfolio(UUID userId, UUID portfolioId) {
        if (hasAllPortfolioAccess(userId)) {
            return true;
        }
        Boolean exists = jdbcTemplate.queryForObject(
                """
                SELECT EXISTS (
                    SELECT 1
                    FROM auth_user_portfolio_access
                    WHERE user_id = ? AND portfolio_id = ?
                )
                """,
                Boolean.class,
                userId,
                portfolioId
        );
        return Boolean.TRUE.equals(exists);
    }

    public void grantPortfolioAccess(UUID userId, UUID portfolioId, AuthSession grantedBy) {
        Instant now = Instant.now();
        jdbcTemplate.update(
                """
                INSERT INTO auth_user_portfolio_access (
                    user_id, portfolio_id, granted_at, granted_by_user_id, granted_by_username, granted_by_display_name
                )
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (user_id, portfolio_id) DO NOTHING
                """,
                userId,
                portfolioId,
                Timestamp.from(now),
                grantedBy.user().id(),
                grantedBy.user().username(),
                grantedBy.user().displayName()
        );
    }
}
