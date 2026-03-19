-- File lifecycle and safety: status (activation gate), safety_class (immutable after ACTIVE), upload_context_key.
ALTER TABLE stored_file
    ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS safety_class VARCHAR(64),
    ADD COLUMN IF NOT EXISTS upload_context_key VARCHAR(64);

-- Backfill existing rows: ACTIVE with general user class.
UPDATE stored_file
SET safety_class = 'GENERAL_USER_ATTACHMENT_ONLY',
    upload_context_key = 'GENERAL_USER_FILE'
WHERE safety_class IS NULL;

COMMENT ON COLUMN stored_file.status IS 'Lifecycle status; only ACTIVE allows bind/download. DELETED is terminal.';
COMMENT ON COLUMN stored_file.safety_class IS 'Assigned after acceptance, immutable once ACTIVE.';
COMMENT ON COLUMN stored_file.upload_context_key IS 'Upload context key used for policy selection.';
