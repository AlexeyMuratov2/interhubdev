package com.example.interhubdev.notification;

import java.time.Instant;
import java.util.List;

/**
 * Container for resolved notification content: one or more notification items
 * (e.g. one per teacher for absence notice events).
 */
public record ResolvedNotificationContent(
        List<ResolvedNotificationItem> items,
        Instant sourceOccurredAt
) {
    public ResolvedNotificationContent {
        if (items == null) {
            throw new IllegalArgumentException("items must not be null");
        }
        if (sourceOccurredAt == null) {
            throw new IllegalArgumentException("sourceOccurredAt must not be null");
        }
    }
}
