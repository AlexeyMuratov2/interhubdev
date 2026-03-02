-- =============================================================================
-- Absence notice: one notice can cover multiple lessons (multi-lesson notices).
-- Add absence_notice_lesson; remove lesson_session_id and teacher response from absence_notice;
-- Allow multiple attendance_record rows to reference the same absence_notice_id.
-- =============================================================================

-- 1. Drop old indexes on absence_notice first (free index names before reusing on new table)
DROP INDEX IF EXISTS uq_absence_notice_active_session_student;
DROP INDEX IF EXISTS idx_absence_notice_lesson_session_id;
DROP INDEX IF EXISTS idx_absence_notice_student_session;

-- 2. Create junction table notice -> lessons
CREATE TABLE absence_notice_lesson (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notice_id UUID NOT NULL REFERENCES absence_notice(id) ON DELETE CASCADE,
    lesson_session_id UUID NOT NULL REFERENCES lesson(id) ON DELETE CASCADE,
    CONSTRAINT uq_absence_notice_lesson_notice_session UNIQUE (notice_id, lesson_session_id)
);

CREATE INDEX IF NOT EXISTS idx_absence_notice_lesson_notice_id ON absence_notice_lesson(notice_id);
CREATE INDEX IF NOT EXISTS idx_absence_notice_lesson_session_id ON absence_notice_lesson(lesson_session_id);

COMMENT ON TABLE absence_notice_lesson IS 'Links absence notice to lesson sessions. One notice can cover multiple lessons.';
COMMENT ON COLUMN absence_notice_lesson.notice_id IS 'Absence notice id (absence_notice.id).';
COMMENT ON COLUMN absence_notice_lesson.lesson_session_id IS 'Lesson id (lesson.id).';

-- 3. Migrate existing data: one row per notice with its lesson_session_id
INSERT INTO absence_notice_lesson (notice_id, lesson_session_id)
SELECT id, lesson_session_id FROM absence_notice WHERE lesson_session_id IS NOT NULL;

-- 4. Drop columns from absence_notice
ALTER TABLE absence_notice
    DROP COLUMN IF EXISTS lesson_session_id,
    DROP COLUMN IF EXISTS teacher_comment,
    DROP COLUMN IF EXISTS responded_at,
    DROP COLUMN IF EXISTS responded_by,
    DROP COLUMN IF EXISTS attached_record_id;

-- 5. Migrate APPROVED/REJECTED to SUBMITTED (historical; no longer used)
UPDATE absence_notice SET status = 'SUBMITTED' WHERE status IN ('APPROVED', 'REJECTED');

-- 6. Restrict status to SUBMITTED and CANCELED only
ALTER TABLE absence_notice DROP CONSTRAINT IF EXISTS absence_notice_status_check;
ALTER TABLE absence_notice ADD CONSTRAINT absence_notice_status_check
    CHECK (status IN ('SUBMITTED', 'CANCELED'));

COMMENT ON TABLE absence_notice IS 'Student absence notices. One notice can cover multiple lessons via absence_notice_lesson. No teacher approval; teachers are notified and may contact student personally.';

-- 7. Allow multiple attendance_record rows to reference the same absence_notice_id
DROP INDEX IF EXISTS uq_attendance_record_absence_notice_id;

COMMENT ON COLUMN attendance_record.absence_notice_id IS 'Optional link to absence_notice.id when teacher attaches student notice to this attendance record. Same notice may be attached to multiple records (one per lesson).';
