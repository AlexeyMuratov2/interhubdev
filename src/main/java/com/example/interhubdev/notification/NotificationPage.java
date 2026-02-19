package com.example.interhubdev.notification;

import java.util.List;
import java.util.UUID;

/**
 * Cursor-based page of notifications.
 */
public record NotificationPage(
        List<NotificationDto> items,
        UUID nextCursor
) {
    /**
     * True if there are more items (next page available).
     */
    public boolean hasNext() {
        return nextCursor != null;
    }
}
