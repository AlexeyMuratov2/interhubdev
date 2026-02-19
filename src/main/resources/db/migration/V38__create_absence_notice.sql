-- =============================================================================
-- Attendance module: Absence Notice subsystem
-- Students can submit notices about absence or lateness for lesson sessions.
-- Supports file attachments (stored file IDs from Document module).
-- =============================================================================

CREATE TABLE absence_notice (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lesson_session_id UUID NOT NULL REFERENCES lesson(id) ON DELETE CASCADE,
    student_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL CHECK (type IN ('ABSENT', 'LATE')),
    reason_text TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED' CHECK (status IN ('SUBMITTED', 'CANCELED', 'ACKNOWLEDGED', 'ATTACHED')),
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    canceled_at TIMESTAMP,
    attached_record_id UUID REFERENCES attendance_record(id) ON DELETE SET NULL,
    version BIGINT DEFAULT 0
);

-- Partial unique index: only one active (SUBMITTED) notice per student per session
CREATE UNIQUE INDEX uq_absence_notice_active_session_student 
    ON absence_notice(lesson_session_id, student_id) 
    WHERE status = 'SUBMITTED';

CREATE INDEX idx_absence_notice_lesson_session_id ON absence_notice(lesson_session_id);
CREATE INDEX idx_absence_notice_student_id_submitted_at ON absence_notice(student_id, submitted_at);
CREATE INDEX idx_absence_notice_student_session ON absence_notice(student_id, lesson_session_id);
CREATE INDEX idx_absence_notice_status ON absence_notice(status);

COMMENT ON TABLE absence_notice IS 'Student absence notices for lesson sessions. One active (SUBMITTED) notice per student per session.';
COMMENT ON COLUMN absence_notice.lesson_session_id IS 'Lesson (session) id (lesson.id).';
COMMENT ON COLUMN absence_notice.student_id IS 'Student profile id (students.id). FK not enforced to allow historical records if student is removed.';
COMMENT ON COLUMN absence_notice.type IS 'Notice type: ABSENT (will be absent) or LATE (will be late).';
COMMENT ON COLUMN absence_notice.reason_text IS 'Optional reason text provided by student (max 2000 chars recommended).';
COMMENT ON COLUMN absence_notice.status IS 'Notice status: SUBMITTED (active), CANCELED (canceled by student), ACKNOWLEDGED (reserved), ATTACHED (reserved for future link to attendance_record).';
COMMENT ON COLUMN absence_notice.submitted_at IS 'Timestamp when notice was first submitted (immutable).';
COMMENT ON COLUMN absence_notice.updated_at IS 'Timestamp when notice was last updated.';
COMMENT ON COLUMN absence_notice.canceled_at IS 'Timestamp when notice was canceled (set when status changes to CANCELED).';
COMMENT ON COLUMN absence_notice.attached_record_id IS 'Reserved for future: link to attendance_record when notice is attached to official attendance mark.';
COMMENT ON COLUMN absence_notice.version IS 'Optimistic locking version (for concurrent updates).';

CREATE TABLE absence_notice_attachment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notice_id UUID NOT NULL REFERENCES absence_notice(id) ON DELETE CASCADE,
    file_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_absence_notice_attachment_notice_file UNIQUE (notice_id, file_id)
);

CREATE INDEX idx_absence_notice_attachment_notice_id ON absence_notice_attachment(notice_id);
CREATE INDEX idx_absence_notice_attachment_file_id ON absence_notice_attachment(file_id);

COMMENT ON TABLE absence_notice_attachment IS 'File attachments for absence notices. References stored files from Document module (file_id is storedFileId UUID).';
COMMENT ON COLUMN absence_notice_attachment.notice_id IS 'Absence notice id (absence_notice.id).';
COMMENT ON COLUMN absence_notice_attachment.file_id IS 'Stored file id from Document module (stored_file.id as UUID string). FK not enforced (cross-module reference).';
COMMENT ON COLUMN absence_notice_attachment.created_at IS 'Timestamp when attachment was added (immutable).';
