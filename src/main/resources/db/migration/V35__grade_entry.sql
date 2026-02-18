-- =============================================================================
-- Grades module: ledger of point allocations per student per offering.
-- Each row is one allocation/correction. Supports soft delete (status VOIDED).
-- =============================================================================

CREATE TABLE grade_entry (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL,
    offering_id UUID NOT NULL REFERENCES group_subject_offering(id) ON DELETE CASCADE,
    points DECIMAL(6,2) NOT NULL,
    type_code VARCHAR(50) NOT NULL,
    type_label VARCHAR(255),
    description TEXT,
    lesson_id UUID REFERENCES lesson(id) ON DELETE SET NULL,
    homework_submission_id UUID REFERENCES homework_submission(id) ON DELETE SET NULL,
    graded_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    graded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    CONSTRAINT chk_grade_entry_status CHECK (status IN ('ACTIVE', 'VOIDED')),
    CONSTRAINT chk_grade_entry_type_label_custom CHECK (
        (type_code = 'CUSTOM' AND type_label IS NOT NULL AND trim(type_label) <> '')
        OR (type_code <> 'CUSTOM' AND type_label IS NULL)
    )
);

CREATE INDEX idx_grade_entry_student_offering_graded_at ON grade_entry(student_id, offering_id, graded_at);
CREATE INDEX idx_grade_entry_offering_graded_at ON grade_entry(offering_id, graded_at);
CREATE INDEX idx_grade_entry_lesson_id ON grade_entry(lesson_id);
CREATE INDEX idx_grade_entry_homework_submission_id ON grade_entry(homework_submission_id);
CREATE INDEX idx_grade_entry_status ON grade_entry(status);

COMMENT ON TABLE grade_entry IS 'Ledger of grade point allocations per student per offering. Each row is one allocation or correction.';
COMMENT ON COLUMN grade_entry.student_id IS 'Student profile id (students.id). FK not enforced to allow historical records if student is removed.';
COMMENT ON COLUMN grade_entry.offering_id IS 'Group subject offering (subject delivery to group).';
COMMENT ON COLUMN grade_entry.points IS 'Points awarded (can be negative for corrections).';
COMMENT ON COLUMN grade_entry.type_code IS 'System type: SEMINAR, EXAM, COURSEWORK, HOMEWORK, OTHER, or CUSTOM.';
COMMENT ON COLUMN grade_entry.type_label IS 'Required when type_code = CUSTOM (teacher-defined label). Must be NULL otherwise.';
COMMENT ON COLUMN grade_entry.lesson_id IS 'Optional link to lesson (session).';
COMMENT ON COLUMN grade_entry.homework_submission_id IS 'Optional link to homework submission; primary reference when present.';
COMMENT ON COLUMN grade_entry.graded_by IS 'User who created or last updated this entry.';
COMMENT ON COLUMN grade_entry.status IS 'ACTIVE = counts in totals; VOIDED = soft-deleted, excluded from sums.';
