-- =============================================================================
-- Student–group n:m: one student can belong to multiple groups.
-- Replaces students.group_id (single group) with student_group_member link table.
-- =============================================================================

-- Link table: student ↔ group (many-to-many)
CREATE TABLE student_group_member (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    group_id UUID NOT NULL REFERENCES student_group(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_student_group_member UNIQUE (student_id, group_id)
);

CREATE INDEX idx_student_group_member_student_id ON student_group_member(student_id);
CREATE INDEX idx_student_group_member_group_id ON student_group_member(group_id);

COMMENT ON TABLE student_group_member IS 'Many-to-many: students can belong to multiple groups';
COMMENT ON COLUMN student_group_member.student_id IS 'Student profile id (students.id)';
COMMENT ON COLUMN student_group_member.group_id IS 'Student group id (student_group.id)';

-- Migrate existing data: one group per student -> one row per student in that group
INSERT INTO student_group_member (id, student_id, group_id)
SELECT gen_random_uuid(), id, group_id
FROM students
WHERE group_id IS NOT NULL;

-- Drop old column and index
DROP INDEX IF EXISTS idx_students_group_id;
ALTER TABLE students
    DROP COLUMN group_id;
