-- =============================================================================
-- Extend assessment_type with additional fields
-- Add new assessment types for curriculum management
-- =============================================================================

-- Add new columns to assessment_type
ALTER TABLE assessment_type ADD COLUMN is_graded BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE assessment_type ADD COLUMN is_final BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE assessment_type ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN assessment_type.is_graded IS 'TRUE = graded with mark, FALSE = pass/fail only';
COMMENT ON COLUMN assessment_type.is_final IS 'TRUE = final semester assessment, FALSE = intermediate';
COMMENT ON COLUMN assessment_type.sort_order IS 'Order for UI display';

-- Insert standard assessment types (if they don't exist)
INSERT INTO assessment_type (code, name, is_graded, is_final, sort_order)
VALUES 
    ('EXAM', 'Exam', TRUE, TRUE, 1),
    ('PASS', 'Pass/Fail', FALSE, TRUE, 2),
    ('DIFF_PASS', 'Differentiated Pass', TRUE, TRUE, 3),
    ('COURSE_WORK', 'Course Work', TRUE, FALSE, 4),
    ('COURSE_PROJECT', 'Course Project', TRUE, FALSE, 5),
    ('MIDTERM', 'Midterm Exam', TRUE, FALSE, 6),
    ('TEST', 'Test', TRUE, FALSE, 7)
ON CONFLICT (code) DO UPDATE SET
    is_graded = EXCLUDED.is_graded,
    is_final = EXCLUDED.is_final,
    sort_order = EXCLUDED.sort_order;

-- Create index for sorting
CREATE INDEX idx_assessment_type_sort_order ON assessment_type(sort_order);

-- =============================================================================
-- Extend curriculum_subject with additional hour types
-- =============================================================================

ALTER TABLE curriculum_subject ADD COLUMN hours_seminar INTEGER;
ALTER TABLE curriculum_subject ADD COLUMN hours_self_study INTEGER;
ALTER TABLE curriculum_subject ADD COLUMN hours_consultation INTEGER;
ALTER TABLE curriculum_subject ADD COLUMN hours_course_work INTEGER;

COMMENT ON COLUMN curriculum_subject.hours_seminar IS 'Seminar hours';
COMMENT ON COLUMN curriculum_subject.hours_self_study IS 'Self-study hours (independent work)';
COMMENT ON COLUMN curriculum_subject.hours_consultation IS 'Consultation hours';
COMMENT ON COLUMN curriculum_subject.hours_course_work IS 'Course work hours';

-- =============================================================================
-- Create curriculum_subject_assessment table for multiple assessments per subject
-- =============================================================================

CREATE TABLE curriculum_subject_assessment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    curriculum_subject_id UUID NOT NULL REFERENCES curriculum_subject(id) ON DELETE CASCADE,
    assessment_type_id UUID NOT NULL REFERENCES assessment_type(id) ON DELETE RESTRICT,
    week_number INTEGER,
    is_final BOOLEAN NOT NULL DEFAULT FALSE,
    weight NUMERIC(3,2),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_curriculum_subject_assessment_weight CHECK (weight IS NULL OR (weight >= 0 AND weight <= 1))
);

CREATE INDEX idx_curriculum_subject_assessment_curriculum_subject_id ON curriculum_subject_assessment(curriculum_subject_id);
CREATE INDEX idx_curriculum_subject_assessment_assessment_type_id ON curriculum_subject_assessment(assessment_type_id);

COMMENT ON TABLE curriculum_subject_assessment IS 'Multiple assessment types per curriculum subject (exam, coursework, midterms, etc.)';
COMMENT ON COLUMN curriculum_subject_assessment.week_number IS 'Week number when assessment takes place (NULL = end of semester)';
COMMENT ON COLUMN curriculum_subject_assessment.is_final IS 'TRUE = final semester assessment, FALSE = intermediate';
COMMENT ON COLUMN curriculum_subject_assessment.weight IS 'Weight in final grade (0.00-1.00), NULL if not weighted';

-- =============================================================================
-- Extend curriculum with status and workflow fields
-- =============================================================================

CREATE TYPE curriculum_status AS ENUM ('DRAFT', 'UNDER_REVIEW', 'APPROVED', 'ARCHIVED');

ALTER TABLE curriculum ADD COLUMN status curriculum_status NOT NULL DEFAULT 'DRAFT';
ALTER TABLE curriculum ADD COLUMN approved_at TIMESTAMP;
ALTER TABLE curriculum ADD COLUMN approved_by UUID REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE curriculum ADD COLUMN end_year INTEGER;

CREATE INDEX idx_curriculum_status ON curriculum(status);
CREATE INDEX idx_curriculum_approved_by ON curriculum(approved_by);

COMMENT ON COLUMN curriculum.status IS 'Curriculum status: DRAFT, UNDER_REVIEW, APPROVED, ARCHIVED';
COMMENT ON COLUMN curriculum.approved_at IS 'When curriculum was approved';
COMMENT ON COLUMN curriculum.approved_by IS 'User who approved the curriculum';
COMMENT ON COLUMN curriculum.end_year IS 'Year when curriculum expires';

-- =============================================================================
-- Create academic_year and semester tables for academic calendar
-- =============================================================================

CREATE TABLE academic_year (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_current BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_academic_year_dates CHECK (end_date > start_date)
);

CREATE UNIQUE INDEX idx_academic_year_name ON academic_year(name);
CREATE INDEX idx_academic_year_is_current ON academic_year(is_current);

COMMENT ON TABLE academic_year IS 'Academic year (e.g. 2025/2026)';
COMMENT ON COLUMN academic_year.name IS 'Academic year name, e.g. 2025/2026';
COMMENT ON COLUMN academic_year.is_current IS 'Only one year should be current';

CREATE TABLE semester (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    academic_year_id UUID NOT NULL REFERENCES academic_year(id) ON DELETE CASCADE,
    number INTEGER NOT NULL,
    name VARCHAR(100),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    exam_start_date DATE,
    exam_end_date DATE,
    week_count INTEGER DEFAULT 16,
    is_current BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_semester_year_number UNIQUE (academic_year_id, number),
    CONSTRAINT chk_semester_dates CHECK (end_date > start_date),
    CONSTRAINT chk_semester_exam_dates CHECK (exam_end_date IS NULL OR exam_start_date IS NULL OR exam_end_date >= exam_start_date),
    CONSTRAINT chk_semester_number CHECK (number >= 1)
);

CREATE INDEX idx_semester_academic_year_id ON semester(academic_year_id);
CREATE INDEX idx_semester_is_current ON semester(is_current);

COMMENT ON TABLE semester IS 'Semester within an academic year';
COMMENT ON COLUMN semester.number IS 'Semester number (1 = Fall, 2 = Spring)';
COMMENT ON COLUMN semester.name IS 'Semester name (e.g. Fall Semester, Spring Semester)';
COMMENT ON COLUMN semester.exam_start_date IS 'Start of exam period';
COMMENT ON COLUMN semester.exam_end_date IS 'End of exam period';
COMMENT ON COLUMN semester.week_count IS 'Number of weeks in semester';
COMMENT ON COLUMN semester.is_current IS 'Only one semester should be current';

-- =============================================================================
-- Create curriculum_practice table for internships and practices
-- =============================================================================

CREATE TYPE practice_type AS ENUM ('EDUCATIONAL', 'INDUSTRIAL', 'PRE_DIPLOMA', 'OTHER');
CREATE TYPE practice_location AS ENUM ('UNIVERSITY', 'ENTERPRISE', 'REMOTE');

CREATE TABLE curriculum_practice (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    curriculum_id UUID NOT NULL REFERENCES curriculum(id) ON DELETE CASCADE,
    practice_type practice_type NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    semester_no INTEGER NOT NULL,
    duration_weeks INTEGER NOT NULL,
    credits NUMERIC(5,2),
    assessment_type_id UUID REFERENCES assessment_type(id) ON DELETE SET NULL,
    location_type practice_location DEFAULT 'ENTERPRISE',
    supervisor_required BOOLEAN NOT NULL DEFAULT TRUE,
    report_required BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_curriculum_practice_semester_no CHECK (semester_no >= 1),
    CONSTRAINT chk_curriculum_practice_duration_weeks CHECK (duration_weeks >= 1)
);

CREATE INDEX idx_curriculum_practice_curriculum_id ON curriculum_practice(curriculum_id);
CREATE INDEX idx_curriculum_practice_assessment_type_id ON curriculum_practice(assessment_type_id);
CREATE INDEX idx_curriculum_practice_semester_no ON curriculum_practice(semester_no);

COMMENT ON TABLE curriculum_practice IS 'Internships and practices within curriculum';
COMMENT ON COLUMN curriculum_practice.practice_type IS 'Type: EDUCATIONAL, INDUSTRIAL, PRE_DIPLOMA, OTHER';
COMMENT ON COLUMN curriculum_practice.location_type IS 'Location: UNIVERSITY, ENTERPRISE, REMOTE';
COMMENT ON COLUMN curriculum_practice.supervisor_required IS 'Whether a supervisor is required';
COMMENT ON COLUMN curriculum_practice.report_required IS 'Whether a report is required';

-- =============================================================================
-- Add department_id to subject table
-- =============================================================================

ALTER TABLE subject ADD COLUMN department_id UUID REFERENCES department(id) ON DELETE SET NULL;

CREATE INDEX idx_subject_department_id ON subject(department_id);

COMMENT ON COLUMN subject.department_id IS 'Department responsible for teaching the subject';
