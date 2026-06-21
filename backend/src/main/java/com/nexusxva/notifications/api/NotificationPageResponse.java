package com.nexusxva.notifications.api;

import com.nexusxva.notifications.application.NotificationInbox;
import java.util.List;

public record NotificationPageResponse(
        List<NotificationResponse> items,
        long unreadCount,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    static NotificationPageResponse from(NotificationInbox inbox) {
        return new NotificationPageResponse(
                inbox.notifications().getContent().stream().map(NotificationResponse::from).toList(),
                inbox.unreadCount(),
                inbox.notifications().getNumber(),
                inbox.notifications().getSize(),
                inbox.notifications().getTotalElements(),
                inbox.notifications().getTotalPages()
        );
    }
}
