package com.nexusxva.notifications.infrastructure;

import com.nexusxva.notifications.application.NotificationStore;
import com.nexusxva.notifications.domain.UserNotification;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class JdbcNotificationStore implements NotificationStore {

    private final JdbcTemplate jdbcTemplate;

    JdbcNotificationStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void createForUser(
            UUID recipientUserId,
            String notificationType,
            String title,
            String message,
            String linkPath,
            Instant createdAt
    ) {
        jdbcTemplate.update(
                """
                INSERT INTO user_notifications (
                    id, recipient_user_id, notification_type, title, message, link_path, created_at, read_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, NULL)
                """,
                UUID.randomUUID(),
                recipientUserId,
                notificationType,
                title,
                message,
                linkPath,
                Timestamp.from(createdAt)
        );
    }

    @Override
    public int createForGroup(
            String groupCode,
            String notificationType,
            String title,
            String message,
            String linkPath,
            Instant createdAt
    ) {
        List<UUID> userIds = findActiveUserIdsByGroup(groupCode);
        userIds.forEach(userId -> createForUser(userId, notificationType, title, message, linkPath, createdAt));
        return userIds.size();
    }

    @Override
    public Page<UserNotification> findForUser(UUID recipientUserId, boolean unreadOnly, Pageable pageable) {
        String unreadClause = unreadOnly ? "AND read_at IS NULL" : "";
        List<UserNotification> items = jdbcTemplate.query(
                """
                SELECT id, recipient_user_id, notification_type, title, message, link_path, created_at, read_at
                FROM user_notifications
                WHERE recipient_user_id = ?
                %s
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
                """.formatted(unreadClause),
                this::mapNotification,
                recipientUserId,
                pageable.getPageSize(),
                pageable.getOffset()
        );
        Long total = jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM user_notifications
                WHERE recipient_user_id = ?
                %s
                """.formatted(unreadClause),
                Long.class,
                recipientUserId
        );
        return new PageImpl<>(items, pageable, total == null ? 0 : total);
    }

    @Override
    public long countUnread(UUID recipientUserId) {
        Long count = jdbcTemplate.queryForObject(
                """
                SELECT count(*)
                FROM user_notifications
                WHERE recipient_user_id = ?
                  AND read_at IS NULL
                """,
                Long.class,
                recipientUserId
        );
        return count == null ? 0 : count;
    }

    @Override
    public Optional<UserNotification> markRead(UUID notificationId, UUID recipientUserId, Instant readAt) {
        jdbcTemplate.update(
                """
                UPDATE user_notifications
                SET read_at = COALESCE(read_at, ?)
                WHERE id = ?
                  AND recipient_user_id = ?
                """,
                Timestamp.from(readAt),
                notificationId,
                recipientUserId
        );
        return findByIdForUser(notificationId, recipientUserId);
    }

    @Override
    public int markAllRead(UUID recipientUserId, Instant readAt) {
        return jdbcTemplate.update(
                """
                UPDATE user_notifications
                SET read_at = ?
                WHERE recipient_user_id = ?
                  AND read_at IS NULL
                """,
                Timestamp.from(readAt),
                recipientUserId
        );
    }

    @Override
    public List<UUID> findActiveUserIdsByGroup(String groupCode) {
        return jdbcTemplate.query(
                """
                SELECT u.id
                FROM auth_user_accounts u
                JOIN auth_user_group_memberships m ON m.user_id = u.id
                JOIN auth_groups g ON g.id = m.group_id
                WHERE g.code = ?
                  AND u.active = TRUE
                ORDER BY u.username
                """,
                (rs, rowNum) -> rs.getObject("id", UUID.class),
                groupCode
        );
    }

    private Optional<UserNotification> findByIdForUser(UUID notificationId, UUID recipientUserId) {
        List<UserNotification> notifications = jdbcTemplate.query(
                """
                SELECT id, recipient_user_id, notification_type, title, message, link_path, created_at, read_at
                FROM user_notifications
                WHERE id = ?
                  AND recipient_user_id = ?
                """,
                this::mapNotification,
                notificationId,
                recipientUserId
        );
        return notifications.stream().findFirst();
    }

    private UserNotification mapNotification(ResultSet rs, int rowNum) throws SQLException {
        Timestamp readAt = rs.getTimestamp("read_at");
        return new UserNotification(
                rs.getObject("id", UUID.class),
                rs.getObject("recipient_user_id", UUID.class),
                rs.getString("notification_type"),
                rs.getString("title"),
                rs.getString("message"),
                rs.getString("link_path"),
                rs.getTimestamp("created_at").toInstant(),
                readAt == null ? null : readAt.toInstant()
        );
    }
}
