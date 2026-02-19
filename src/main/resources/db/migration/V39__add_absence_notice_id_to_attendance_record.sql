-- =============================================================================
-- Attendance module: Link absence notices to attendance records.
-- Single source of truth: attendance_record.absence_notice_id references absence_notice.id
-- =============================================================================

ALTER TABLE attendance_record
    ADD COLUMN absence_notice_id UUID;

-- Index for fast lookups
CREATE INDEX idx_attendance_record_absence_notice_id ON attendance_record(absence_notice_id);

-- Unique constraint: one notice can be attached to only one record
-- This prevents double-attachment of the same notice
CREATE UNIQUE INDEX uq_attendance_record_absence_notice_id 
    ON attendance_record(absence_notice_id) 
    WHERE absence_notice_id IS NOT NULL;

COMMENT ON COLUMN attendance_record.absence_notice_id IS 'Optional link to absence_notice.id when teacher attaches student notice to attendance record. Single source of truth: link is stored here, not in absence_notice.attached_record_id.';
