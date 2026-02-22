-- =============================================================================
-- Homework: multiple files per assignment (junction table homework_file).
-- Migrate existing homework.stored_file_id into homework_file, then drop column.
-- =============================================================================

CREATE TABLE homework_file (
    homework_id UUID NOT NULL REFERENCES homework(id) ON DELETE CASCADE,
    stored_file_id UUID NOT NULL REFERENCES stored_file(id) ON DELETE RESTRICT,
    sort_order INT,
    PRIMARY KEY (homework_id, stored_file_id)
);

CREATE INDEX idx_homework_file_homework_id ON homework_file(homework_id);
CREATE INDEX idx_homework_file_stored_file_id ON homework_file(stored_file_id);

COMMENT ON TABLE homework_file IS 'Junction: one homework has many stored files.';
COMMENT ON COLUMN homework_file.sort_order IS 'Display order of files in the homework.';

-- Migrate existing data: one row per homework that has a non-null stored_file_id
INSERT INTO homework_file (homework_id, stored_file_id, sort_order)
SELECT id, stored_file_id, 0
FROM homework
WHERE stored_file_id IS NOT NULL;

-- Drop the old column and its index
DROP INDEX IF EXISTS idx_homework_stored_file_id;
ALTER TABLE homework DROP COLUMN stored_file_id;
