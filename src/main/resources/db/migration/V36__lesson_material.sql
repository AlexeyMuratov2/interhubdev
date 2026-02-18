-- =============================================================================
-- Lesson materials: materials for a specific lesson (one lesson many materials,
-- one material many files via lesson_material_file).
-- =============================================================================

CREATE TABLE lesson_material (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lesson_id UUID NOT NULL REFERENCES lesson(id) ON DELETE CASCADE,
    name VARCHAR(500) NOT NULL,
    description TEXT,
    author_id UUID NOT NULL REFERENCES users(id),
    published_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_lesson_material_lesson_id ON lesson_material(lesson_id);
CREATE INDEX idx_lesson_material_author_id ON lesson_material(author_id);

COMMENT ON TABLE lesson_material IS 'Materials for a specific lesson. One lesson has many materials.';
COMMENT ON COLUMN lesson_material.lesson_id IS 'FK to lesson (schedule).';
COMMENT ON COLUMN lesson_material.name IS 'Material name/title.';
COMMENT ON COLUMN lesson_material.description IS 'Optional description.';
COMMENT ON COLUMN lesson_material.author_id IS 'User who created the material.';
COMMENT ON COLUMN lesson_material.published_at IS 'When the material was published.';

CREATE TABLE lesson_material_file (
    lesson_material_id UUID NOT NULL REFERENCES lesson_material(id) ON DELETE CASCADE,
    stored_file_id UUID NOT NULL REFERENCES stored_file(id) ON DELETE RESTRICT,
    sort_order INT,
    PRIMARY KEY (lesson_material_id, stored_file_id)
);

CREATE INDEX idx_lesson_material_file_material_id ON lesson_material_file(lesson_material_id);
CREATE INDEX idx_lesson_material_file_stored_file_id ON lesson_material_file(stored_file_id);

COMMENT ON TABLE lesson_material_file IS 'Junction: one lesson material has many stored files.';
COMMENT ON COLUMN lesson_material_file.sort_order IS 'Display order of files in the material.';
