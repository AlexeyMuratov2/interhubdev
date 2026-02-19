# Notification Module

In-app notification inbox for users. Provides notification storage, retrieval, and automatic creation from outbox events.

## Overview

The Notification module implements a clean architecture with clear boundaries:
- **Domain** - Notification entity, value objects, domain rules
- **Application** - Use cases (create, read, mark as read), ports
- **API** - REST controllers, DTOs
- **Infrastructure** - Repositories, DB entities, outbox handlers

## Notification Design

Notifications use **client-side localization**:
- **templateKey** - English key (e.g., `attendance.absenceNotice.submitted`)
- **params** - JSON object with data for template rendering
- **data** - JSON object for deep-linking and navigation

Backend does **not** store ready text (title/body), only templateKey + params + data.

## Template Keys

Current template keys (see `NotificationTemplateKeys`):

### `attendance.absenceNotice.submitted`
**Recipients:** Teachers of the lesson session  
**When:** Student submits a new absence notice

**Params:**
- `sessionId` (UUID string)
- `noticeId` (UUID string)
- `studentId` (UUID string)
- `noticeType` (`ABSENT` | `LATE`)

**Data:**
- `route`: `"sessionAttendance"`
- `sessionId` (UUID string)
- `focus`: `"notices"`
- `noticeId` (UUID string)
- `studentId` (UUID string)

### `attendance.absenceNotice.updated`
**Recipients:** Teachers of the lesson session  
**When:** Student updates an existing absence notice

**Params:** Same as `attendance.absenceNotice.submitted`  
**Data:** Same as `attendance.absenceNotice.submitted`

### `attendance.record.marked`
**Recipients:** Student  
**When:** Attendance is marked for a student

**Params:**
- `sessionId` (UUID string)
- `recordId` (UUID string)
- `status` (`PRESENT` | `ABSENT` | `LATE` | `EXCUSED`)

**Data:**
- `route`: `"studentAttendance"`
- `sessionId` (UUID string)
- `recordId` (UUID string)

## API Endpoints

### GET /api/notifications/mine
Get notifications for current user with cursor pagination.

**Query parameters:**
- `status` (optional): `"all"` or `"unread"` (default: `"all"`)
- `cursor` (optional): UUID cursor from previous page
- `limit` (optional): Max items per page (default: 30, max: 50)

**Response:** `NotificationPage` with `items` and `nextCursor`

### GET /api/notifications/mine/unread-count
Get count of unread notifications for current user.

**Response:** `{ "count": <number> }`

### POST /api/notifications/{id}/read
Mark a specific notification as read. Idempotent.

**Response:** `204 No Content`

### POST /api/notifications/mine/read-all
Mark all notifications as read for current user. Idempotent.

**Response:** `204 No Content`

## Idempotency

Notification creation is **idempotent**: unique constraint on `(recipient_user_id, source_event_id)` prevents duplicates when outbox events are retried.

If a notification already exists for a `(recipientUserId, sourceEventId)` pair, the handler silently ignores the duplicate (treats as success).

## Outbox Event Handlers

The module registers handlers for the following Attendance events:

- `attendance.absence_notice.submitted` → `AbsenceNoticeSubmittedHandler`
- `attendance.absence_notice.updated` → `AbsenceNoticeUpdatedHandler`
- `attendance.record.marked` → `AttendanceMarkedHandler`

### Handler Flow

1. Parse event payload
2. Resolve recipients:
   - For absence notices: Get lesson → offering → teachers → user IDs
   - For attendance marked: Get student → user ID
3. Create notification for each recipient
4. Save with idempotency protection

### TODO: Future Channels

In handlers, there are TODO comments for future mobile push/email delivery:

```java
// TODO: In future, after creating in-app notification, enqueue push delivery (mobile) based on user preferences.
```

These are architectural extension points. Nothing is implemented yet.

## Database Schema

Table: `notification`

**Fields:**
- `id` (UUID, PK)
- `recipient_user_id` (UUID, NOT NULL)
- `template_key` (VARCHAR(255), NOT NULL)
- `params_json` (JSONB, NOT NULL)
- `data_json` (JSONB, NOT NULL)
- `created_at` (TIMESTAMP, NOT NULL)
- `read_at` (TIMESTAMP, nullable)
- `archived_at` (TIMESTAMP, nullable)
- `source_event_id` (UUID, NOT NULL)
- `source_event_type` (VARCHAR(255), NOT NULL)
- `source_occurred_at` (TIMESTAMP, NOT NULL)

**Constraints:**
- Unique: `(recipient_user_id, source_event_id)`

**Indexes:**
- `(recipient_user_id, read_at, created_at DESC)` - for querying user notifications
- `(recipient_user_id, created_at DESC)` - for pagination
- `(source_event_id)` - for debugging
- `(template_key, created_at)` - for analytics (optional)

## Dependencies

- `error` - Error handling
- `outbox` - Event handler registration
- `auth` - Authentication in controllers
- `schedule` - Lesson/session lookup
- `offering` - Offering and teacher lookup
- `teacher` - Teacher-to-user mapping
- `student` - Student-to-user mapping

## Testing

See:
- `NotificationRepositoryTest` - Repository integration tests
- Handler integration tests (TODO: add comprehensive handler tests)

## Future Enhancements

1. **Mobile Push Notifications** - Enqueue push delivery after creating in-app notification
2. **Email Notifications** - Send email for important notifications
3. **Notification Preferences** - User preferences for notification channels
4. **Notification Templates** - Backend template rendering (optional, currently client-side)
5. **Notification Groups** - Group related notifications
6. **Rich Notifications** - Images, actions, etc.
