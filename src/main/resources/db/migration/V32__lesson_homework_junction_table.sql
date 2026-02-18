-- =============================================================================
-- Create junction table for lesson-homework relationship with FK constraints
-- =============================================================================

-- Create junction table with FK to lesson and homework
CREATE TABLE lesson_homework (
    lesson_id UUID NOT NULL REFERENCES lesson(id) ON DELETE CASCADE,
    homework_id UUID NOT NULL REFERENCES homework(id) ON DELETE CASCADE,
    PRIMARY KEY (lesson_id, homework_id)
);

-- Migrate existing data from homework.lesson_id to junction table
INSERT INTO lesson_homework (lesson_id, homework_id)
SELECT lesson_id, id
FROM homework
WHERE lesson_id IS NOT NULL;

-- Create index on homework_id for efficient lookups
CREATE INDEX idx_lesson_homework_homework_id ON lesson_homework(homework_id);
CREATE INDEX idx_lesson_homework_lesson_id ON lesson_homework(lesson_id);

-- Add unique constraint on homework_id to ensure one-to-many relationship
-- (each homework is linked to exactly one lesson)
ALTER TABLE lesson_homework ADD CONSTRAINT uk_lesson_homework_homework_id UNIQUE (homework_id);

-- Remove lesson_id column from homework table (data is now in junction table)
ALTER TABLE homework DROP COLUMN lesson_id;

COMMENT ON TABLE lesson_homework IS 'Junction table linking lessons to homework assignments. Each homework is linked to exactly one lesson.';
COMMENT ON COLUMN lesson_homework.lesson_id IS 'Lesson UUID (FK to lesson table)';
COMMENT ON COLUMN lesson_homework.homework_id IS 'Homework UUID (FK to homework table, unique to ensure one-to-many)';
