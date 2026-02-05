-- =============================================================================
-- Change group curator from teacher-only to any user (admin, staff, teacher).
-- =============================================================================

-- Add new column referencing users
ALTER TABLE student_group
    ADD COLUMN curator_user_id UUID REFERENCES users(id) ON DELETE SET NULL;

-- Migrate existing data: map teacher id -> user id
UPDATE student_group sg
SET curator_user_id = t.user_id
FROM teachers t
WHERE sg.curator_teacher_id = t.id;

-- Drop old index and column
DROP INDEX IF EXISTS idx_student_group_curator_teacher_id;
ALTER TABLE student_group
    DROP COLUMN curator_teacher_id;

-- Index for curator lookups
CREATE INDEX idx_student_group_curator_user_id ON student_group(curator_user_id);

COMMENT ON COLUMN student_group.curator_user_id IS 'Curator (advisor) of the group - any user (admin, staff, teacher)';
