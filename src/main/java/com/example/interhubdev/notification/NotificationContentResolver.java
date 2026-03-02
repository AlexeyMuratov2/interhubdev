package com.example.interhubdev.notification;

import java.util.Map;
import java.util.Optional;

/**
 * Port for resolving notification content from integration event payloads.
 * <p>
 * Implemented by the adapter module; allows notification handlers to create rich
 * notifications (e.g. absence notice submitted) without depending on student, schedule,
 * offering, teacher modules. The adapter aggregates data from those modules and returns
 * ready-to-use content per recipient.
 */
public interface NotificationContentResolver {

    /**
     * Resolve notification content for the given event type and payload.
     *
     * @param eventType integration event type (e.g. "attendance.absence_notice.submitted")
     * @param payload   deserialized event payload (Map from JSON)
     * @return resolved content with one item per recipient, or empty if this event type is not supported
     */
    Optional<ResolvedNotificationContent> resolve(String eventType, Map<String, Object> payload);
}
