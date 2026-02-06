-- =============================================================================
-- OfferingSlot owns time (day_of_week, start_time, end_time); timeslot_id optional.
-- =============================================================================

ALTER TABLE offering_slot ADD COLUMN day_of_week INTEGER;
ALTER TABLE offering_slot ADD COLUMN start_time TIME;
ALTER TABLE offering_slot ADD COLUMN end_time TIME;

UPDATE offering_slot os
SET day_of_week = t.day_of_week, start_time = t.start_time, end_time = t.end_time
FROM timeslot t
WHERE t.id = os.timeslot_id;

ALTER TABLE offering_slot ALTER COLUMN day_of_week SET NOT NULL;
ALTER TABLE offering_slot ALTER COLUMN start_time SET NOT NULL;
ALTER TABLE offering_slot ALTER COLUMN end_time SET NOT NULL;

ALTER TABLE offering_slot DROP CONSTRAINT IF EXISTS offering_slot_timeslot_id_fkey;
ALTER TABLE offering_slot DROP CONSTRAINT IF EXISTS offering_slot_offering_id_timeslot_id_lesson_type_key;

ALTER TABLE offering_slot ALTER COLUMN timeslot_id DROP NOT NULL;
ALTER TABLE offering_slot ADD CONSTRAINT offering_slot_timeslot_id_fkey
    FOREIGN KEY (timeslot_id) REFERENCES timeslot(id) ON DELETE SET NULL;

CREATE UNIQUE INDEX offering_slot_offering_day_start_end_type_key
    ON offering_slot (offering_id, day_of_week, start_time, end_time, lesson_type);

COMMENT ON COLUMN offering_slot.day_of_week IS 'Day of week 1..7 (slot owns time).';
COMMENT ON COLUMN offering_slot.start_time IS 'Slot start time.';
COMMENT ON COLUMN offering_slot.end_time IS 'Slot end time.';
COMMENT ON COLUMN offering_slot.timeslot_id IS 'Optional reference to timeslot used when creating (UI hint).';
