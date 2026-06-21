package com.nexusxva.notifications.api;

import com.nexusxva.auth.domain.AuthSession;
import com.nexusxva.auth.infrastructure.AuthSessionFilter;
import com.nexusxva.notifications.application.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/api/notifications")
    public NotificationPageResponse inbox(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request
    ) {
        return NotificationPageResponse.from(notificationService.inbox(currentUserId(request), unreadOnly, page, size));
    }

    @PostMapping("/api/notifications/{notificationId}/read")
    public NotificationResponse markRead(@PathVariable UUID notificationId, HttpServletRequest request) {
        return NotificationResponse.from(notificationService.markRead(notificationId, currentUserId(request)));
    }

    @PostMapping("/api/notifications/read-all")
    public ResponseEntity<Void> markAllRead(HttpServletRequest request) {
        notificationService.markAllRead(currentUserId(request));
        return ResponseEntity.noContent().build();
    }

    private UUID currentUserId(HttpServletRequest request) {
        AuthSession session = (AuthSession) request.getAttribute(AuthSessionFilter.SESSION_ATTRIBUTE);
        return session.user().id();
    }
}
