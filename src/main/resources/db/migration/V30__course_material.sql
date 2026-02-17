-- Course material: business entity linking subject to stored file.
-- CourseMaterial is separate from StoredFile to keep file storage generic.
CREATE TABLE course_material (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject_id UUID NOT NULL,
    stored_file_id UUID NOT NULL REFERENCES stored_file(id) ON DELETE RESTRICT,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    author_id UUID NOT NULL REFERENCES users(id),
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_course_material_subject_file UNIQUE (subject_id, stored_file_id)
);

CREATE INDEX idx_course_material_subject_id ON course_material(subject_id);
CREATE INDEX idx_course_material_stored_file_id ON course_material(stored_file_id);
CREATE INDEX idx_course_material_author_id ON course_material(author_id);

COMMENT ON TABLE course_material IS 'Course materials (files) linked to subjects. References stored_file for actual file content.';
COMMENT ON COLUMN course_material.subject_id IS 'Subject UUID (no FK constraint - subject module may not exist yet)';
COMMENT ON COLUMN course_material.stored_file_id IS 'Reference to stored_file table (file metadata and S3 path)';
COMMENT ON COLUMN course_material.title IS 'Material title (e.g., "Lecture 1: Introduction")';
COMMENT ON COLUMN course_material.description IS 'Optional description of the material';
COMMENT ON COLUMN course_material.author_id IS 'User who uploaded/created this material';
COMMENT ON COLUMN course_material.uploaded_at IS 'When this material was uploaded (may differ from stored_file.uploaded_at)';
