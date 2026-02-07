-- =============================================================================
-- Lesson: add foreign key on offering_slot_id for referential integrity.
-- ON DELETE SET NULL: if slot is deleted (e.g. directly in DB), lesson keeps
-- existing with offering_slot_id = null. Application deletes lessons before
-- deleting slot, so this is a safety net.
-- =============================================================================

ALTER TABLE lesson
    ADD CONSTRAINT lesson_offering_slot_id_fkey
    FOREIGN KEY (offering_slot_id) REFERENCES offering_slot(id) ON DELETE SET NULL;
