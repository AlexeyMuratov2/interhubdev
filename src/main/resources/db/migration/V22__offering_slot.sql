CREATE TABLE offering_slot (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    offering_id     UUID NOT NULL REFERENCES group_subject_offering(id) ON DELETE CASCADE,
    timeslot_id     UUID NOT NULL REFERENCES timeslot(id),
    lesson_type     VARCHAR(50) NOT NULL,
    room_id         UUID REFERENCES room(id),
    teacher_id      UUID,
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (offering_id, timeslot_id, lesson_type)
);
