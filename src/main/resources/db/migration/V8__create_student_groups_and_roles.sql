-- =============================================================================
-- University schema: Student groups and group roles (headman, deputy)
-- student_group links to program and curriculum; students reference group.
-- =============================================================================

-- Student group: concrete group following a program and curriculum
CREATE TABLE student_group (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    program_id UUID NOT NULL REFERENCES program(id) ON DELETE RESTRICT,
    curriculum_id UUID NOT NULL REFERENCES curriculum(id) ON DELETE RESTRICT,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255),
    description TEXT,
    start_year INTEGER NOT NULL,
    graduation_year INTEGER,
    curator_teacher_id UUID REFERENCES teachers(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_student_group_code ON student_group(code);
CREATE INDEX idx_student_group_program_id ON student_group(program_id);
CREATE INDEX idx_student_group_curriculum_id ON student_group(curriculum_id);
CREATE INDEX idx_student_group_curator_teacher_id ON student_group(curator_teacher_id);
CREATE INDEX idx_student_group_start_year ON student_group(start_year);

COMMENT ON TABLE student_group IS 'Concrete student group following a program and curriculum. Layer 2–3 link.';
COMMENT ON COLUMN student_group.code IS 'Unique group code, e.g. IU-21-1';
COMMENT ON COLUMN student_group.curator_teacher_id IS 'Curator (advisor) of the group';

-- Add group_id to students (nullable for backward compatibility; existing rows keep group_name)
ALTER TABLE students
    ADD COLUMN group_id UUID REFERENCES student_group(id) ON DELETE SET NULL;

CREATE INDEX idx_students_group_id ON students(group_id);
COMMENT ON COLUMN students.group_id IS 'Reference to student_group. Replaces/supplements group_name.';

-- Group leader roles (headman, deputy) with history
CREATE TABLE group_leader (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL REFERENCES student_group(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    from_date DATE,
    to_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_group_leader_group_student_role UNIQUE (group_id, student_id, role),
    CONSTRAINT chk_group_leader_role CHECK (role IN ('headman', 'deputy'))
);

CREATE INDEX idx_group_leader_group_id ON group_leader(group_id);
CREATE INDEX idx_group_leader_student_id ON group_leader(student_id);
CREATE INDEX idx_group_leader_dates ON group_leader(from_date, to_date);

COMMENT ON TABLE group_leader IS 'Group roles (headman, deputy) with optional date range for history.';
COMMENT ON COLUMN group_leader.role IS 'headman = староста, deputy = заместитель';
COMMENT ON COLUMN group_leader.from_date IS 'Start of tenure (NULL = from creation)';
COMMENT ON COLUMN group_leader.to_date IS 'End of tenure (NULL = current)';
