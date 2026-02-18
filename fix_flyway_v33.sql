-- =============================================================================
-- Fix Flyway migration V33: remove old migration record and allow new one to apply
-- Execute this SQL script manually in your PostgreSQL database before restarting the app
-- =============================================================================

-- Delete the old V33 migration record from flyway_schema_history
-- This allows Flyway to apply the new V33 migration
DELETE FROM flyway_schema_history WHERE version = '33';

-- Optional: Check if lesson_course_material table exists and drop it
-- (the new migration will handle this, but you can do it manually if needed)
DROP TABLE IF EXISTS lesson_course_material;
