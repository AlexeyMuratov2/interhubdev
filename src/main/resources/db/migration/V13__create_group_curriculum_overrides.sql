-- =============================================================================
-- University schema: Group curriculum overrides
-- Add/remove/replace subjects for a group without cloning the full curriculum.
-- =============================================================================

CREATE TYPE group_curriculum_override_action AS ENUM ('ADD', 'REMOVE', 'REPLACE');

CREATE TABLE group_curriculum_override (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL REFERENCES student_group(id) ON DELETE CASCADE,
    curriculum_subject_id UUID REFERENCES curriculum_subject(id) ON DELETE SET NULL,
    subject_id UUID REFERENCES subject(id) ON DELETE SET NULL,
    action group_curriculum_override_action NOT NULL,
    new_assessment_type_id UUID REFERENCES assessment_type(id) ON DELETE SET NULL,
    new_duration_weeks INTEGER,
    reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_override_subject_ref CHECK (
        (action = 'REMOVE' AND curriculum_subject_id IS NOT NULL AND subject_id IS NULL)
        OR (action = 'ADD' AND subject_id IS NOT NULL AND curriculum_subject_id IS NULL)
        OR (action = 'REPLACE' AND curriculum_subject_id IS NOT NULL)
        -- for REPLACE: subject_id optional (replace with another subject or just change params)
    )
);

CREATE INDEX idx_group_curriculum_override_group_id ON group_curriculum_override(group_id);
CREATE INDEX idx_group_curriculum_override_curriculum_subject_id ON group_curriculum_override(curriculum_subject_id);
CREATE INDEX idx_group_curriculum_override_subject_id ON group_curriculum_override(subject_id);
CREATE INDEX idx_group_curriculum_override_created_at ON group_curriculum_override(created_at);

COMMENT ON TABLE group_curriculum_override IS 'Overrides for a group: add/remove/replace subjects without cloning curriculum.';
COMMENT ON COLUMN group_curriculum_override.curriculum_subject_id IS 'Existing curriculum subject (for REMOVE or REPLACE)';
COMMENT ON COLUMN group_curriculum_override.subject_id IS 'New subject (for ADD)';
COMMENT ON COLUMN group_curriculum_override.action IS 'ADD: add subject; REMOVE: drop subject; REPLACE: change subject params';
