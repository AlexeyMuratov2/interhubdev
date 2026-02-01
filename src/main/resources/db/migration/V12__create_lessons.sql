-- =============================================================================
-- University schema: Lessons (schedule instances)
-- Concrete lesson: date + timeslot + room for an offering.
-- =============================================================================

CREATE TABLE lesson (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    offering_id UUID NOT NULL REFERENCES group_subject_offering(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    timeslot_id UUID NOT NULL REFERENCES timeslot(id) ON DELETE RESTRICT,
    room_id UUID REFERENCES room(id) ON DELETE SET NULL,
    topic VARCHAR(500),
    status VARCHAR(50) NOT NULL DEFAULT 'planned',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_lesson_status CHECK (status IN ('planned', 'cancelled', 'done'))
);

CREATE INDEX idx_lesson_offering_id ON lesson(offering_id);
CREATE INDEX idx_lesson_date ON lesson(date);
CREATE INDEX idx_lesson_timeslot_id ON lesson(timeslot_id);
CREATE INDEX idx_lesson_room_id ON lesson(room_id);
CREATE INDEX idx_lesson_status ON lesson(status);

COMMENT ON TABLE lesson IS 'Single scheduled lesson: date + timeslot + room. room_id may differ from offering default.';
COMMENT ON COLUMN lesson.topic IS 'Optional topic for this lesson';
COMMENT ON COLUMN lesson.status IS 'planned, cancelled, or done';
