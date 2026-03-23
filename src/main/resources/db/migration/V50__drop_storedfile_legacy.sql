-- =============================================================================
-- Remove legacy stored_file-based business bindings after attachment cutover.
-- =============================================================================

DROP TABLE IF EXISTS homework_submission_file;
DROP TABLE IF EXISTS lesson_material_file;
DROP TABLE IF EXISTS homework_file;

ALTER TABLE course_material DROP CONSTRAINT IF EXISTS uk_course_material_offering_file;
DROP INDEX IF EXISTS idx_course_material_stored_file_id;
ALTER TABLE course_material DROP COLUMN IF EXISTS stored_file_id;

DROP TABLE IF EXISTS stored_file;
