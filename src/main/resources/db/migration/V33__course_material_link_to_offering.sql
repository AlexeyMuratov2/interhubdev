-- =============================================================================
-- Change course_material to link to group_subject_offering instead of subject
-- This allows materials to belong to a specific offering (group + curriculum_subject + teacher)
-- =============================================================================

-- Drop the junction table if it exists (from previous migration)
DROP TABLE IF EXISTS lesson_course_material;

-- Drop unique constraint on subject_id + stored_file_id
ALTER TABLE course_material DROP CONSTRAINT IF EXISTS uk_course_material_subject_file;

-- Add new column for offering_id
ALTER TABLE course_material ADD COLUMN offering_id UUID REFERENCES group_subject_offering(id) ON DELETE CASCADE;

-- Migrate existing data: try to find offering by subject_id
-- Note: This migration assumes that for each subject_id there's at least one offering
-- If multiple offerings exist for a subject, we'll link to the first one found
-- If no offering exists, offering_id will be NULL (these materials should be reviewed manually)
UPDATE course_material cm
SET offering_id = (
    SELECT gso.id
    FROM group_subject_offering gso
    JOIN curriculum_subject cs ON gso.curriculum_subject_id = cs.id
    WHERE cs.subject_id = cm.subject_id
    LIMIT 1
);

-- Make offering_id NOT NULL after migration
-- Note: If some materials couldn't be migrated, you'll need to handle them manually
ALTER TABLE course_material ALTER COLUMN offering_id SET NOT NULL;

-- Drop old subject_id column
ALTER TABLE course_material DROP COLUMN subject_id;

-- Create new unique constraint on offering_id + stored_file_id
ALTER TABLE course_material ADD CONSTRAINT uk_course_material_offering_file UNIQUE (offering_id, stored_file_id);

-- Update indexes
DROP INDEX IF EXISTS idx_course_material_subject_id;
CREATE INDEX idx_course_material_offering_id ON course_material(offering_id);

-- Update comments
COMMENT ON TABLE course_material IS 'Course materials (files) linked to group_subject_offering (specific delivery of subject to group). References stored_file for actual file content.';
COMMENT ON COLUMN course_material.offering_id IS 'Group subject offering UUID (FK to group_subject_offering table)';
