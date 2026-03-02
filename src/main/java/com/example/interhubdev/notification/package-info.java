/**
 * Notification module - in-app notification inbox for users.
 * <p>
 * Provides notification storage, retrieval, and outbox event handlers for creating
 * notifications from domain events (e.g., attendance events).
 * <p>
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.notification.NotificationApi} - main interface for notification operations</li>
 *   <li>{@link com.example.interhubdev.notification.NotificationDto} - notification data transfer object</li>
 *   <li>{@link com.example.interhubdev.notification.NotificationPage} - cursor-based pagination for notifications</li>
 *   <li>{@link com.example.interhubdev.notification.NotificationTemplateKeys} - template key constants</li>
 *   <li>{@link com.example.interhubdev.notification.NotificationContentResolver} - port for resolving rich notification content from event payloads (implemented by adapter)</li>
 *   <li>{@link com.example.interhubdev.notification.ResolvedNotificationContent} - container for resolved items and source timestamp</li>
 *   <li>{@link com.example.interhubdev.notification.ResolvedNotificationItem} - single notification item (recipient, templateKey, params, data)</li>
 * </ul>
 * <p>
 * <h2>Architecture</h2>
 * <p>
 * The module follows clean architecture with clear boundaries:
 * <ul>
 *   <li><b>Domain</b> - Notification entity, value objects, domain rules</li>
 *   <li><b>Application</b> - Use cases (create, read, mark as read), ports</li>
 *   <li><b>API</b> - REST controllers, DTOs</li>
 *   <li><b>Infrastructure</b> - Repositories, DB entities, outbox handlers</li>
 * </ul>
 * <p>
 * <h2>Notification Design</h2>
 * <p>
 * Notifications use client-side localization:
 * <ul>
 *   <li><b>templateKey</b> - English key (e.g., "attendance.absenceNotice.submitted")</li>
 *   <li><b>params</b> - JSON object with data for template rendering</li>
 *   <li><b>data</b> - JSON object for deep-linking and navigation</li>
 * </ul>
 * <p>
 * Backend does not store ready text (title/body), only templateKey + params + data.
 * <p>
 * <h2>Idempotency</h2>
 * <p>
 * Notification creation is idempotent: unique constraint on (recipient_user_id, source_event_id)
 * prevents duplicates when outbox events are retried.
 * <p>
 * <h2>Dependencies</h2>
 * <ul>
 *   <li>error - for error handling</li>
 *   <li>outbox - for event handler registration</li>
 *   <li>auth - for authentication in controllers</li>
 *   <li>attendancerecord - for attendance-marked handler (student userId lookup)</li>
 *   <li>student - for attendance-marked handler</li>
 * </ul>
 * <p>
 * Rich notification content for absence notice events is resolved via {@link com.example.interhubdev.notification.NotificationContentResolver}
 * (implemented in the adapter module); notification does not depend on schedule, offering, teacher or absencenotice for that flow.
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Notification",
    allowedDependencies = {"error", "outbox", "auth", "attendancerecord", "student"}
)
package com.example.interhubdev.notification;
