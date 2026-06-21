package com.nexusxva.notifications.application;

import com.nexusxva.notifications.domain.UserNotification;
import org.springframework.data.domain.Page;

public record NotificationInbox(
        Page<UserNotification> notifications,
        long unreadCount
) {
}
