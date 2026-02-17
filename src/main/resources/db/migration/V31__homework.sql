-- Homework: assignment linked to a lesson. Optional file reference; clearing reference does not delete the file.
CREATE TABLE homework (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lesson_id UUID NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    points INTEGER,
    stored_file_id UUID REFERENCES stored_file(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_homework_lesson_id ON homework(lesson_id);
CREATE INDEX idx_homework_stored_file_id ON homework(stored_file_id);

COMMENT ON TABLE homework IS 'Homework assignments linked to lessons. File reference is optional; removing it does not delete the stored file.';
COMMENT ON COLUMN homework.lesson_id IS 'Lesson UUID (validated via application; no FK to keep document module independent of schedule)';
COMMENT ON COLUMN homework.title IS 'Homework title';
COMMENT ON COLUMN homework.description IS 'Optional description';
COMMENT ON COLUMN homework.points IS 'Optional max points for this homework';
COMMENT ON COLUMN homework.stored_file_id IS 'Optional reference to attached file; clearing reference does not delete the file';
