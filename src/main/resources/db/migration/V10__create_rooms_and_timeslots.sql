-- =============================================================================
-- University schema: Rooms and timeslots (foundation for scheduling)
-- =============================================================================

CREATE TABLE room (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    building VARCHAR(100) NOT NULL,
    number VARCHAR(50) NOT NULL,
    capacity INTEGER,
    type VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_room_type CHECK (type IS NULL OR type IN ('lecture', 'lab', 'practice'))
);

CREATE INDEX idx_room_building ON room(building);
CREATE INDEX idx_room_type ON room(type);
COMMENT ON TABLE room IS 'Classroom or lab. type: lecture, lab, practice.';

CREATE TABLE timeslot (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    day_of_week INTEGER NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_timeslot_day CHECK (day_of_week >= 1 AND day_of_week <= 7),
    CONSTRAINT chk_timeslot_time CHECK (end_time > start_time)
);

CREATE INDEX idx_timeslot_day_of_week ON timeslot(day_of_week);
COMMENT ON TABLE timeslot IS 'Weekly slot: day 1..7, start_time, end_time.';
