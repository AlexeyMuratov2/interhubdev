-- Outbox event table for transactional outbox pattern
-- Ensures reliable delivery of integration events

CREATE TABLE outbox_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(255) NOT NULL,
    payload_json JSONB NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'NEW',
    attempts INTEGER NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP,
    locked_by VARCHAR(255),
    locked_at TIMESTAMP,
    last_error TEXT,
    processed_at TIMESTAMP,
    correlation_id VARCHAR(255),
    trace_id VARCHAR(255),
    
    CONSTRAINT chk_status CHECK (status IN ('NEW', 'PROCESSING', 'DONE', 'FAILED'))
);

-- Primary index for selecting events ready for processing
-- Used by OutboxProcessor to find NEW/FAILED events ready for retry
CREATE INDEX idx_outbox_event_status_next_retry_occurred 
    ON outbox_event(status, next_retry_at, occurred_at) 
    WHERE status IN ('NEW', 'FAILED');

-- Index for occurred_at (for ordering and analytics)
CREATE INDEX idx_outbox_event_occurred_at ON outbox_event(occurred_at);

-- Index for event_type and occurred_at (for analytics and filtering)
CREATE INDEX idx_outbox_event_type_occurred ON outbox_event(event_type, occurred_at);

-- Index for correlation_id (for tracing)
CREATE INDEX idx_outbox_event_correlation_id ON outbox_event(correlation_id) WHERE correlation_id IS NOT NULL;

-- Index for trace_id (for distributed tracing)
CREATE INDEX idx_outbox_event_trace_id ON outbox_event(trace_id) WHERE trace_id IS NOT NULL;

COMMENT ON TABLE outbox_event IS 'Transactional outbox table for reliable integration event delivery';
COMMENT ON COLUMN outbox_event.event_type IS 'Event type identifier (e.g., attendance.absence_notice_submitted)';
COMMENT ON COLUMN outbox_event.payload_json IS 'Event payload as JSON (serialized from domain object)';
COMMENT ON COLUMN outbox_event.occurred_at IS 'When the event occurred in the domain (domain event timestamp)';
COMMENT ON COLUMN outbox_event.created_at IS 'When the event was written to outbox';
COMMENT ON COLUMN outbox_event.status IS 'NEW - not processed yet, PROCESSING - currently being processed, DONE - successfully processed, FAILED - processing failed';
COMMENT ON COLUMN outbox_event.attempts IS 'Number of processing attempts';
COMMENT ON COLUMN outbox_event.next_retry_at IS 'When to retry processing (exponential backoff)';
COMMENT ON COLUMN outbox_event.locked_by IS 'Worker instance identifier that locked this event';
COMMENT ON COLUMN outbox_event.locked_at IS 'When the event was locked for processing';
COMMENT ON COLUMN outbox_event.last_error IS 'Last error message if processing failed';
COMMENT ON COLUMN outbox_event.processed_at IS 'When the event was successfully processed';
COMMENT ON COLUMN outbox_event.correlation_id IS 'Correlation ID for request tracing';
COMMENT ON COLUMN outbox_event.trace_id IS 'Distributed trace ID';
