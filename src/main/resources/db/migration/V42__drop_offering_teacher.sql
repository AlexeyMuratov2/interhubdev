-- Teachers for an offering are now derived from main teacher (group_subject_offering.teacher_id)
-- and slot teachers (offering_slot.teacher_id); no separate offering_teacher table.
DROP TABLE IF EXISTS offering_teacher;
