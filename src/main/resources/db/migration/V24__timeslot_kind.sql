-- =============================================================================
-- Timeslot kind: STANDARD (schedule grid), EVENT, CONSULTATION, EXAM, CUSTOM
-- =============================================================================

ALTER TABLE timeslot
    ADD COLUMN kind VARCHAR(20) NOT NULL DEFAULT 'STANDARD';

ALTER TABLE timeslot
    ADD CONSTRAINT chk_timeslot_kind CHECK (kind IN ('STANDARD', 'EVENT', 'CONSULTATION', 'EXAM', 'CUSTOM'));

CREATE INDEX idx_timeslot_kind ON timeslot(kind);
COMMENT ON COLUMN timeslot.kind IS 'STANDARD = main schedule grid; EVENT, CONSULTATION, EXAM, CUSTOM = non-standard slots.';
