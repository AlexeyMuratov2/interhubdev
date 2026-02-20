-- =============================================================================
-- Refactor curriculum: replace start_year and end_year with duration_years
-- =============================================================================

-- Add new column duration_years
ALTER TABLE curriculum ADD COLUMN duration_years INTEGER;

-- Migrate data: calculate duration_years from start_year and end_year
-- If end_year is NULL, use default value of 4 years
UPDATE curriculum
SET duration_years = COALESCE(end_year - start_year + 1, 4)
WHERE duration_years IS NULL;

-- Make duration_years NOT NULL after data migration
ALTER TABLE curriculum ALTER COLUMN duration_years SET NOT NULL;

-- Drop old columns
ALTER TABLE curriculum DROP COLUMN start_year;
ALTER TABLE curriculum DROP COLUMN end_year;

-- Drop index on start_year (no longer exists)
DROP INDEX IF EXISTS idx_curriculum_start_year;

-- Update comments
COMMENT ON COLUMN curriculum.duration_years IS 'Duration of the curriculum in years (e.g., 4 for a 4-year program). Start year comes from the group using this curriculum.';
