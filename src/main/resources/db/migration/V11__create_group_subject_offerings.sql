-- =============================================================================
-- University schema: Group-specific delivery (offering, offering_teacher)
-- How a subject is delivered to a specific group: teacher, room, format.
-- =============================================================================

CREATE TABLE group_subject_offering (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL REFERENCES student_group(id) ON DELETE CASCADE,
    curriculum_subject_id UUID NOT NULL REFERENCES curriculum_subject(id) ON DELETE RESTRICT,
    teacher_id UUID REFERENCES teachers(id) ON DELETE SET NULL,
    room_id UUID REFERENCES room(id) ON DELETE SET NULL,
    format VARCHAR(50),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_group_subject_offering_group_curriculum_subject UNIQUE (group_id, curriculum_subject_id),
    CONSTRAINT chk_offering_format CHECK (format IS NULL OR format IN ('offline', 'online', 'mixed'))
);

CREATE INDEX idx_group_subject_offering_group_id ON group_subject_offering(group_id);
CREATE INDEX idx_group_subject_offering_curriculum_subject_id ON group_subject_offering(curriculum_subject_id);
CREATE INDEX idx_group_subject_offering_teacher_id ON group_subject_offering(teacher_id);
CREATE INDEX idx_group_subject_offering_room_id ON group_subject_offering(room_id);

COMMENT ON TABLE group_subject_offering IS 'How a curriculum subject is delivered to a specific group (teacher, room, format). Layer 3.';
COMMENT ON COLUMN group_subject_offering.format IS 'offline, online, or mixed';

-- Multiple teachers per offering (lecture / practice / lab)
CREATE TABLE offering_teacher (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    offering_id UUID NOT NULL REFERENCES group_subject_offering(id) ON DELETE CASCADE,
    teacher_id UUID NOT NULL REFERENCES teachers(id) ON DELETE RESTRICT,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_offering_teacher_offering_teacher_role UNIQUE (offering_id, teacher_id, role),
    CONSTRAINT chk_offering_teacher_role CHECK (role IN ('LECTURE', 'PRACTICE', 'LAB'))
);

CREATE INDEX idx_offering_teacher_offering_id ON offering_teacher(offering_id);
CREATE INDEX idx_offering_teacher_teacher_id ON offering_teacher(teacher_id);

COMMENT ON TABLE offering_teacher IS 'Additional teachers for an offering (e.g. lecture vs practice vs lab).';
