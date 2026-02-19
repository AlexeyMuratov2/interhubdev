-- =============================================================================
-- Attendance module: official attendance records marked by teachers for lesson sessions.
-- One record per student per lesson session (unique constraint).
-- =============================================================================

CREATE TABLE attendance_record (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lesson_session_id UUID NOT NULL REFERENCES lesson(id) ON DELETE CASCADE,
    student_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PRESENT', 'ABSENT', 'LATE', 'EXCUSED')),
    minutes_late INTEGER,
    teacher_comment TEXT,
    marked_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    marked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_attendance_record_session_student UNIQUE (lesson_session_id, student_id),
    CONSTRAINT chk_attendance_record_minutes_late CHECK (
        (status = 'LATE' AND minutes_late IS NOT NULL AND minutes_late >= 0)
        OR (status <> 'LATE' AND minutes_late IS NULL)
    )
);

CREATE INDEX idx_attendance_record_lesson_session_id ON attendance_record(lesson_session_id);
CREATE INDEX idx_attendance_record_student_id_marked_at ON attendance_record(student_id, marked_at);
CREATE INDEX idx_attendance_record_student_session ON attendance_record(student_id, lesson_session_id);

COMMENT ON TABLE attendance_record IS 'Official attendance records marked by teachers. One record per student per lesson session.';
COMMENT ON COLUMN attendance_record.lesson_session_id IS 'Lesson (session) id (lesson.id).';
COMMENT ON COLUMN attendance_record.student_id IS 'Student profile id (students.id). FK not enforced to allow historical records if student is removed.';
COMMENT ON COLUMN attendance_record.status IS 'Attendance status: PRESENT, ABSENT, LATE (requires minutes_late), EXCUSED.';
COMMENT ON COLUMN attendance_record.minutes_late IS 'Required when status = LATE (minutes student was late). Must be NULL otherwise.';
COMMENT ON COLUMN attendance_record.teacher_comment IS 'Optional teacher comment/explanation (e.g. reason for EXCUSED status).';
COMMENT ON COLUMN attendance_record.marked_by IS 'User id of teacher/admin who marked attendance.';
COMMENT ON COLUMN attendance_record.marked_at IS 'Timestamp when attendance was marked (or last updated).';
COMMENT ON COLUMN attendance_record.updated_at IS 'Timestamp when record was last updated.';
