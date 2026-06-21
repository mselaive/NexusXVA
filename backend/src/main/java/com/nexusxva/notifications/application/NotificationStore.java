package com.nexusxva.notifications.application;

import com.nexusxva.notifications.domain.UserNotification;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationStore {

    void createForUser(
            UUID recipientUserId,
            String notificationType,
            String title,
            String message,
            String linkPath,
            Instant createdAt
    );

    int createForGroup(
            String groupCode,
            String notificationType,
            String title,
            String message,
            String linkPath,
            Instant createdAt
    );

    Page<UserNotification> findForUser(UUID recipientUserId, boolean unreadOnly, Pageable pageable);

    long countUnread(UUID recipientUserId);

    Optional<UserNotification> markRead(UUID notificationId, UUID recipientUserId, Instant readAt);

    int markAllRead(UUID recipientUserId, Instant readAt);

    List<UUID> findActiveUserIdsByGroup(String groupCode);
}
