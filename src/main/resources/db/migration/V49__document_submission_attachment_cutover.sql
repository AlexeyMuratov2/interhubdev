-- =============================================================================
-- Cut over document/submission business bindings from legacy stored_file links
-- to file_asset-backed attachment tables.
-- =============================================================================

CREATE TABLE document_attachment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_type VARCHAR(32) NOT NULL,
    owner_id UUID NOT NULL,
    file_asset_id UUID NOT NULL REFERENCES file_asset(id) ON DELETE RESTRICT,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_document_attachment_owner ON document_attachment(owner_type, owner_id);
CREATE INDEX idx_document_attachment_file_asset_id ON document_attachment(file_asset_id);
CREATE UNIQUE INDEX uk_document_attachment_owner_file_asset
    ON document_attachment(owner_type, owner_id, file_asset_id);

CREATE TABLE submission_attachment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    submission_id UUID NOT NULL REFERENCES homework_submission(id) ON DELETE CASCADE,
    file_asset_id UUID NOT NULL REFERENCES file_asset(id) ON DELETE RESTRICT,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_submission_attachment_submission_id ON submission_attachment(submission_id);
CREATE INDEX idx_submission_attachment_file_asset_id ON submission_attachment(file_asset_id);
CREATE UNIQUE INDEX uk_submission_attachment_submission_file_asset
    ON submission_attachment(submission_id, file_asset_id);

INSERT INTO file_asset (
    id,
    policy_key,
    policy_version,
    status,
    original_name,
    declared_content_type,
    detected_content_type,
    size_bytes,
    uploaded_by,
    final_object_key,
    safety_class,
    delivery_profile,
    archive_profile,
    created_at,
    uploaded_at,
    activated_at,
    claimed_at
)
SELECT
    sf.id,
    'CONTROLLED_ATTACHMENT',
    2,
    'ACTIVE',
    COALESCE(NULLIF(sf.original_name, ''), sf.id::text),
    sf.content_type,
    NULL,
    sf.size,
    sf.uploaded_by,
    sf.storage_path,
    'CONTROLLED_ATTACHMENT_ONLY',
    'BACKEND_ATTACHMENT_STREAM_ONLY',
    'OPAQUE_NO_SERVER_EXTRACTION',
    sf.uploaded_at,
    sf.uploaded_at,
    sf.uploaded_at,
    sf.uploaded_at
FROM stored_file sf
WHERE NOT EXISTS (
    SELECT 1
    FROM file_asset fa
    WHERE fa.id = sf.id
);

INSERT INTO document_attachment (id, owner_type, owner_id, file_asset_id, sort_order, created_at)
SELECT
    gen_random_uuid(),
    'HOMEWORK',
    hf.homework_id,
    hf.stored_file_id,
    COALESCE(hf.sort_order, 0),
    CURRENT_TIMESTAMP
FROM homework_file hf;

INSERT INTO document_attachment (id, owner_type, owner_id, file_asset_id, sort_order, created_at)
SELECT
    gen_random_uuid(),
    'LESSON_MATERIAL',
    lmf.lesson_material_id,
    lmf.stored_file_id,
    COALESCE(lmf.sort_order, 0),
    CURRENT_TIMESTAMP
FROM lesson_material_file lmf;

INSERT INTO document_attachment (id, owner_type, owner_id, file_asset_id, sort_order, created_at)
SELECT
    gen_random_uuid(),
    'COURSE_MATERIAL',
    cm.id,
    cm.stored_file_id,
    0,
    CURRENT_TIMESTAMP
FROM course_material cm;

INSERT INTO submission_attachment (id, submission_id, file_asset_id, sort_order, created_at)
SELECT
    gen_random_uuid(),
    hsf.submission_id,
    hsf.stored_file_id,
    COALESCE(hsf.sort_order, 0),
    CURRENT_TIMESTAMP
FROM homework_submission_file hsf;
