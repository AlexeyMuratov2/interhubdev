-- Stored file metadata (S3 path, size, content type). File content is in object storage.
CREATE TABLE stored_file (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    storage_path VARCHAR(1024) NOT NULL UNIQUE,
    size BIGINT NOT NULL,
    content_type VARCHAR(255),
    original_name VARCHAR(512),
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    uploaded_by UUID REFERENCES users(id)
);

CREATE INDEX idx_stored_file_uploaded_by ON stored_file(uploaded_by);

COMMENT ON TABLE stored_file IS 'Metadata for files stored in S3/MinIO. Used by document module for uploads (homework, lesson materials later).';
