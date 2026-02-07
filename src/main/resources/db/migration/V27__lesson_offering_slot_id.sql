-- =============================================================================
-- Lesson: add offering_slot_id to reference the offering slot this lesson was
-- generated from (for lesson type and teacher resolution on UI).
-- =============================================================================

ALTER TABLE lesson ADD COLUMN offering_slot_id UUID NULL;

CREATE INDEX idx_lesson_offering_slot_id ON lesson(offering_slot_id);

COMMENT ON COLUMN lesson.offering_slot_id IS 'Optional reference to offering slot this lesson was generated from (for lesson type and teacher on UI). Null for manually created or legacy lessons.';
