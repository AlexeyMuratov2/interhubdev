-- =============================================================================
-- Fileasset module: canonical technical file lifecycle aggregate.
-- This table is introduced in parallel with legacy stored_file and does not
-- change public HTTP contracts yet.
-- =============================================================================

CREATE TABLE file_asset (
    id UUID PRIMARY KEY,
    policy_key VARCHAR(64) NOT NULL,
    policy_version INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    original_name VARCHAR(512) NOT NULL,
    declared_content_type VARCHAR(255),
    detected_content_type VARCHAR(255),
    size_bytes BIGINT NOT NULL,
    checksum VARCHAR(255),
    etag VARCHAR(255),
    upload_receipt_token VARCHAR(255),
    uploaded_by UUID REFERENCES users(id),
    temp_object_key VARCHAR(1024),
    final_object_key VARCHAR(1024),
    safety_class VARCHAR(64),
    delivery_profile VARCHAR(64),
    archive_profile VARCHAR(64),
    processing_attempts INT NOT NULL DEFAULT 0,
    last_failure_code VARCHAR(128),
    last_failure_message VARCHAR(1000),
    expires_at TIMESTAMP,
    claimed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    uploaded_at TIMESTAMP,
    activated_at TIMESTAMP,
    failed_at TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_file_asset_status ON file_asset(status);
CREATE INDEX idx_file_asset_uploaded_by ON file_asset(uploaded_by);
CREATE INDEX idx_file_asset_policy_key ON file_asset(policy_key);
CREATE INDEX idx_file_asset_expires_at ON file_asset(expires_at);
CREATE INDEX idx_file_asset_created_at ON file_asset(created_at);

COMMENT ON TABLE file_asset IS 'Canonical file lifecycle aggregate owned by the fileasset module.';
COMMENT ON COLUMN file_asset.policy_key IS 'External selector for internal file security and processing policy.';
COMMENT ON COLUMN file_asset.policy_version IS 'Policy definition version fixed at register time.';
COMMENT ON COLUMN file_asset.status IS 'Lifecycle status: REGISTERED, UPLOADED, PROCESSING, ACTIVE, FAILED, DELETED, EXPIRED.';
COMMENT ON COLUMN file_asset.temp_object_key IS 'Internal temporary storage coordinate; never exposed outside fileasset.';
COMMENT ON COLUMN file_asset.final_object_key IS 'Internal final storage coordinate; never exposed outside fileasset.';
COMMENT ON COLUMN file_asset.claimed_at IS 'Timestamp when a business module confirmed the asset was successfully bound.';
