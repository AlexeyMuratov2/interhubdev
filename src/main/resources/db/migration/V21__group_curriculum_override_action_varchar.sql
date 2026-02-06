-- =============================================================================
-- Use VARCHAR for group_curriculum_override.action so JPA String mapping matches.
-- Drop CHECK first (it compares action to literals; enum->varchar would fail on revalidate).
-- =============================================================================

ALTER TABLE group_curriculum_override
    DROP CONSTRAINT chk_override_subject_ref;

ALTER TABLE group_curriculum_override
    ALTER COLUMN action TYPE VARCHAR(20) USING action::text;

ALTER TABLE group_curriculum_override
    ADD CONSTRAINT chk_override_subject_ref CHECK (
        (action = 'REMOVE' AND curriculum_subject_id IS NOT NULL AND subject_id IS NULL)
        OR (action = 'ADD' AND subject_id IS NOT NULL AND curriculum_subject_id IS NULL)
        OR (action = 'REPLACE' AND curriculum_subject_id IS NOT NULL)
    );

DROP TYPE group_curriculum_override_action;
