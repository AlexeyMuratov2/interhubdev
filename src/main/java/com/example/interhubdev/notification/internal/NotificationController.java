package com.example.interhubdev.notification.internal;

import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.notification.NotificationApi;
import com.example.interhubdev.notification.NotificationPage;
import com.example.interhubdev.user.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for notification endpoints.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notification inbox")
class NotificationController {

    private final NotificationApi notificationApi;
    private final AuthApi authApi;

    /**
     * Get current authenticated user ID.
     */
    private UUID requireCurrentUser(HttpServletRequest request) {
        return authApi.getCurrentUser(request)
                .map(UserDto::id)
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));
    }

    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my notifications", description = "Get notifications for current user with cursor pagination. Status filter: 'all' or 'unread'. Max 50 per page.")
    public ResponseEntity<NotificationPage> getMyNotifications(
            @RequestParam(required = false, defaultValue = "all") String status,
            @RequestParam(required = false) UUID cursor,
            @RequestParam(required = false, defaultValue = "30") int limit,
            HttpServletRequest request
    ) {
        UUID userId = requireCurrentUser(request);
        NotificationPage page = notificationApi.getMyNotifications(userId, status, cursor, limit);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/mine/unread-count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get unread count", description = "Get count of unread notifications for current user")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(HttpServletRequest request) {
        UUID userId = requireCurrentUser(request);
        long count = notificationApi.getUnreadCount(userId);
        return ResponseEntity.ok(new UnreadCountResponse(count));
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark notification as read", description = "Mark a specific notification as read. Idempotent.")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID id,
            HttpServletRequest request
    ) {
        UUID userId = requireCurrentUser(request);
        notificationApi.markAsRead(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/mine/read-all")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark all as read", description = "Mark all notifications as read for current user. Idempotent.")
    public ResponseEntity<Void> markAllAsRead(HttpServletRequest request) {
        UUID userId = requireCurrentUser(request);
        notificationApi.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Response DTO for unread count.
     */
    record UnreadCountResponse(long count) {
    }
}
