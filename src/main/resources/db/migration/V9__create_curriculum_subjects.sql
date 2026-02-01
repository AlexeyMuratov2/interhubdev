-- =============================================================================
-- University schema: Subjects within curriculum (curriculum_subject)
-- Links curriculum to subjects with semester, hours, assessment type.
-- =============================================================================

CREATE TABLE curriculum_subject (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    curriculum_id UUID NOT NULL REFERENCES curriculum(id) ON DELETE RESTRICT,
    subject_id UUID NOT NULL REFERENCES subject(id) ON DELETE RESTRICT,
    semester_no INTEGER NOT NULL,
    course_year INTEGER,
    duration_weeks INTEGER NOT NULL,
    hours_total INTEGER,
    hours_lecture INTEGER,
    hours_practice INTEGER,
    hours_lab INTEGER,
    assessment_type_id UUID NOT NULL REFERENCES assessment_type(id) ON DELETE RESTRICT,
    is_elective BOOLEAN DEFAULT FALSE,
    credits NUMERIC(5, 2),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_curriculum_subject_curriculum_subject_semester UNIQUE (curriculum_id, subject_id, semester_no),
    CONSTRAINT chk_curriculum_subject_semester_no CHECK (semester_no >= 1),
    CONSTRAINT chk_curriculum_subject_duration_weeks CHECK (duration_weeks >= 1)
);

CREATE INDEX idx_curriculum_subject_curriculum_id ON curriculum_subject(curriculum_id);
CREATE INDEX idx_curriculum_subject_subject_id ON curriculum_subject(subject_id);
CREATE INDEX idx_curriculum_subject_assessment_type_id ON curriculum_subject(assessment_type_id);
CREATE INDEX idx_curriculum_subject_semester_no ON curriculum_subject(semester_no);

COMMENT ON TABLE curriculum_subject IS 'Subject as part of a specific curriculum: semester, hours, assessment.';
COMMENT ON COLUMN curriculum_subject.semester_no IS 'Semester number (1..N)';
COMMENT ON COLUMN curriculum_subject.course_year IS 'Course year (1..N), optional';
COMMENT ON COLUMN curriculum_subject.duration_weeks IS 'Duration of the subject in weeks (required)';
COMMENT ON COLUMN curriculum_subject.is_elective IS 'Whether the subject is elective';
