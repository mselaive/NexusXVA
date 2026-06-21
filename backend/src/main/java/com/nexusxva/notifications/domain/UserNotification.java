package com.nexusxva.notifications.domain;

import java.time.Instant;
import java.util.UUID;

public record UserNotification(
        UUID id,
        UUID recipientUserId,
        String notificationType,
        String title,
        String message,
        String linkPath,
        Instant createdAt,
        Instant readAt
) {
    public boolean unread() {
        return readAt == null;
    }
}
