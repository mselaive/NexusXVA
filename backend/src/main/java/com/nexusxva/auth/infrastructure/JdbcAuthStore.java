package com.nexusxva.auth.infrastructure;

import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.domain.AuthenticatedUser;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcAuthStore {

    private final JdbcTemplate jdbcTemplate;

    public JdbcAuthStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<StoredUser> findStoredUserByUsername(String username) {
        List<StoredUser> users = jdbcTemplate.query(
                """
                SELECT id, username, display_name, password_hash, active
                FROM auth_user_accounts
                WHERE lower(username) = lower(?)
                """,
                (rs, rowNum) -> storedUser(rs),
                username
        );
        return users.stream().findFirst();
    }

    public Optional<AuthSession> findSessionByTokenHash(String tokenHash) {
        List<AuthSession> sessions = jdbcTemplate.query(
                """
                SELECT s.id AS session_id, s.active_group_code, s.csrf_token, s.expires_at,
                       u.id AS user_id, u.username, u.display_name
                FROM auth_sessions s
                JOIN auth_user_accounts u ON u.id = s.user_id
                WHERE s.token_hash = ?
                  AND s.revoked_at IS NULL
                  AND s.expires_at > CURRENT_TIMESTAMP
                  AND u.active = TRUE
                """,
                (rs, rowNum) -> new AuthSession(
                        rs.getObject("session_id", UUID.class),
                        new AuthenticatedUser(
                                rs.getObject("user_id", UUID.class),
                                rs.getString("username"),
                                rs.getString("display_name"),
                                groupsForUser(rs.getObject("user_id", UUID.class))
                        ),
                        rs.getString("active_group_code"),
                        rs.getString("csrf_token"),
                        rs.getTimestamp("expires_at").toInstant()
                ),
                tokenHash
        );
        return sessions.stream().findFirst();
    }

    public void createSession(UUID sessionId, UUID userId, String tokenHash, String csrfToken, Instant createdAt, Instant expiresAt) {
        jdbcTemplate.update(
                """
                INSERT INTO auth_sessions (id, user_id, token_hash, csrf_token, created_at, expires_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                sessionId,
                userId,
                tokenHash,
                csrfToken,
                java.sql.Timestamp.from(createdAt),
                java.sql.Timestamp.from(expiresAt)
        );
        jdbcTemplate.update("UPDATE auth_user_accounts SET last_login_at = ?, updated_at = ? WHERE id = ?",
                java.sql.Timestamp.from(createdAt),
                java.sql.Timestamp.from(createdAt),
                userId);
    }

    public void revokeSession(String tokenHash) {
        jdbcTemplate.update(
                "UPDATE auth_sessions SET revoked_at = CURRENT_TIMESTAMP WHERE token_hash = ? AND revoked_at IS NULL",
                tokenHash
        );
    }

    public void updateActiveGroup(UUID sessionId, String activeGroup) {
        jdbcTemplate.update(
                "UPDATE auth_sessions SET active_group_code = ? WHERE id = ? AND revoked_at IS NULL",
                activeGroup,
                sessionId
        );
    }

    public boolean userExists(String username) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM auth_user_accounts WHERE lower(username) = lower(?))",
                Boolean.class,
                username
        );
        return Boolean.TRUE.equals(exists);
    }

    public void createBootstrapAdmin(UUID userId, String username, String displayName, String passwordHash, Instant now) {
        jdbcTemplate.update(
                """
                INSERT INTO auth_user_accounts (id, username, display_name, password_hash, active, created_at, updated_at)
                VALUES (?, ?, ?, ?, TRUE, ?, ?)
                """,
                userId,
                username,
                displayName,
                passwordHash,
                java.sql.Timestamp.from(now),
                java.sql.Timestamp.from(now)
        );
        jdbcTemplate.update(
                """
                INSERT INTO auth_user_group_memberships (user_id, group_id)
                SELECT ?, id
                FROM auth_groups
                WHERE code IN ('ADMIN', 'FO', 'BO')
                """,
                userId
        );
    }

    public List<String> groupsForUser(UUID userId) {
        return jdbcTemplate.query(
                """
                SELECT g.code
                FROM auth_groups g
                JOIN auth_user_group_memberships m ON m.group_id = g.id
                WHERE m.user_id = ?
                ORDER BY g.code
                """,
                (rs, rowNum) -> rs.getString("code"),
                userId
        );
    }

    private StoredUser storedUser(ResultSet rs) throws SQLException {
        UUID id = rs.getObject("id", UUID.class);
        return new StoredUser(
                id,
                rs.getString("username"),
                rs.getString("display_name"),
                rs.getString("password_hash"),
                rs.getBoolean("active"),
                groupsForUser(id)
        );
    }

    public record StoredUser(
            UUID id,
            String username,
            String displayName,
            String passwordHash,
            boolean active,
            List<String> groups
    ) {
    }
}
