package com.nexusxva.notifications.api;

import com.nexusxva.notifications.domain.UserNotification;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        String notificationType,
        String title,
        String message,
        String linkPath,
        boolean unread,
        Instant createdAt,
        Instant readAt
) {
    static NotificationResponse from(UserNotification notification) {
        return new NotificationResponse(
                notification.id(),
                notification.notificationType(),
                notification.title(),
                notification.message(),
                notification.linkPath(),
                notification.unread(),
                notification.createdAt(),
                notification.readAt()
        );
    }
}
