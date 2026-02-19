-- Notification table for in-app notification inbox
-- Stores notifications created from outbox events

CREATE TABLE notification (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_user_id UUID NOT NULL,
    template_key VARCHAR(255) NOT NULL,
    params_json JSONB NOT NULL,
    data_json JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP,
    archived_at TIMESTAMP,
    source_event_id UUID NOT NULL,
    source_event_type VARCHAR(255) NOT NULL,
    source_occurred_at TIMESTAMP NOT NULL,
    
    CONSTRAINT uk_notification_recipient_source_event UNIQUE (recipient_user_id, source_event_id)
);

-- Index for querying user notifications (most common query)
-- Used for: get my notifications ordered by created_at DESC
CREATE INDEX idx_notification_recipient_read_created 
    ON notification(recipient_user_id, read_at, created_at DESC);

-- Index for querying user notifications by created_at (for pagination)
CREATE INDEX idx_notification_recipient_created 
    ON notification(recipient_user_id, created_at DESC);

-- Index for source event lookup (for debugging and deduplication checks)
CREATE INDEX idx_notification_source_event_id 
    ON notification(source_event_id);

-- Index for template key analytics (optional, for future analytics)
CREATE INDEX idx_notification_template_key_created 
    ON notification(template_key, created_at);

COMMENT ON TABLE notification IS 'In-app notification inbox for users';
COMMENT ON COLUMN notification.recipient_user_id IS 'User ID who receives this notification';
COMMENT ON COLUMN notification.template_key IS 'Template key for client-side localization (e.g., attendance.absenceNotice.submitted)';
COMMENT ON COLUMN notification.params_json IS 'JSON object with parameters for template rendering';
COMMENT ON COLUMN notification.data_json IS 'JSON object with data for deep-linking and navigation';
COMMENT ON COLUMN notification.created_at IS 'When the notification was created';
COMMENT ON COLUMN notification.read_at IS 'When the notification was marked as read (null if unread)';
COMMENT ON COLUMN notification.archived_at IS 'When the notification was archived (null if not archived)';
COMMENT ON COLUMN notification.source_event_id IS 'ID of the outbox_event that created this notification';
COMMENT ON COLUMN notification.source_event_type IS 'Type of the outbox event (for diagnostics/analytics)';
COMMENT ON COLUMN notification.source_occurred_at IS 'When the source event occurred (for ordering/debugging)';
