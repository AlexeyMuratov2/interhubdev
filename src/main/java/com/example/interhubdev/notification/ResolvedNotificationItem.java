package com.example.interhubdev.notification;

import java.util.Map;
import java.util.UUID;

/**
 * Single notification to create: recipient, template key, and params/data for the template.
 */
public record ResolvedNotificationItem(
        UUID recipientUserId,
        String templateKey,
        Map<String, Object> params,
        Map<String, Object> data
) {
    public ResolvedNotificationItem {
        if (recipientUserId == null) {
            throw new IllegalArgumentException("recipientUserId must not be null");
        }
        if (templateKey == null || templateKey.isBlank()) {
            throw new IllegalArgumentException("templateKey must not be blank");
        }
        if (params == null) {
            throw new IllegalArgumentException("params must not be null");
        }
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
    }
}
