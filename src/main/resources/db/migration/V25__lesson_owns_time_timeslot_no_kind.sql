-- =============================================================================
-- Lesson owns time (start_time, end_time); timeslot_id optional, UI hint only.
-- Timeslot: remove kind (slots are time templates only).
-- =============================================================================

-- --- Lesson: add time columns, backfill from timeslot, timeslot_id nullable ---
ALTER TABLE lesson ADD COLUMN start_time TIME;
ALTER TABLE lesson ADD COLUMN end_time TIME;

UPDATE lesson l
SET start_time = t.start_time, end_time = t.end_time
FROM timeslot t
WHERE t.id = l.timeslot_id;

ALTER TABLE lesson ALTER COLUMN start_time SET NOT NULL;
ALTER TABLE lesson ALTER COLUMN end_time SET NOT NULL;

ALTER TABLE lesson DROP CONSTRAINT lesson_timeslot_id_fkey;
ALTER TABLE lesson ALTER COLUMN timeslot_id DROP NOT NULL;
ALTER TABLE lesson ADD CONSTRAINT lesson_timeslot_id_fkey
    FOREIGN KEY (timeslot_id) REFERENCES timeslot(id) ON DELETE SET NULL;

CREATE INDEX idx_lesson_start_time ON lesson(start_time);
COMMENT ON COLUMN lesson.start_time IS 'Lesson start time (lesson owns time; timeslot_id is optional UI hint).';
COMMENT ON COLUMN lesson.end_time IS 'Lesson end time.';
COMMENT ON COLUMN lesson.timeslot_id IS 'Optional reference to timeslot used when creating (UI hint).';

-- --- Timeslot: remove kind (slots are time templates only) ---
ALTER TABLE timeslot DROP CONSTRAINT IF EXISTS chk_timeslot_kind;
DROP INDEX IF EXISTS idx_timeslot_kind;
ALTER TABLE timeslot DROP COLUMN IF EXISTS kind;
