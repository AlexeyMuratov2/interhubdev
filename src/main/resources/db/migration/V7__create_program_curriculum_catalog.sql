-- =============================================================================
-- University schema: Program & Curriculum catalog (Layer 1–2)
-- Program = abstract educational program (template).
-- Curriculum = versioned study plan for a program (for a specific intake/year).
-- =============================================================================

-- Optional: department for program
CREATE TABLE department (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_department_code ON department(code);
COMMENT ON TABLE department IS 'Faculty/department. Optional link for programs.';

-- Program: abstract educational program (template)
CREATE TABLE program (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    degree_level VARCHAR(50),
    department_id UUID REFERENCES department(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_program_code ON program(code);
CREATE INDEX idx_program_department_id ON program(department_id);
COMMENT ON TABLE program IS 'Abstract educational program (template). Layer 1.';
COMMENT ON COLUMN program.degree_level IS 'e.g. Bachelor, Master, PhD';
COMMENT ON COLUMN program.department_id IS 'Optional: department responsible for the program';

-- Assessment type: EXAM, PASS, DIFF_PASS, PROJECT, etc.
CREATE TABLE assessment_type (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_assessment_type_code ON assessment_type(code);
COMMENT ON TABLE assessment_type IS 'Type of assessment: exam, pass/fail, differentiated pass, project, etc.';

-- Subject: abstract discipline (catalog)
CREATE TABLE subject (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX idx_subject_code ON subject(code);
COMMENT ON TABLE subject IS 'Abstract discipline (catalog). Used in curriculum_subject.';

-- Curriculum: versioned study plan for a program
CREATE TABLE curriculum (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    program_id UUID NOT NULL REFERENCES program(id) ON DELETE RESTRICT,
    version VARCHAR(50) NOT NULL,
    start_year INTEGER NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_curriculum_program_version UNIQUE (program_id, version)
);

CREATE INDEX idx_curriculum_program_id ON curriculum(program_id);
CREATE INDEX idx_curriculum_start_year ON curriculum(start_year);
CREATE INDEX idx_curriculum_is_active ON curriculum(is_active);
COMMENT ON TABLE curriculum IS 'Versioned study plan for a program (for a specific intake/year). Layer 2.';
COMMENT ON COLUMN curriculum.version IS 'e.g. 2026-v1';
COMMENT ON COLUMN curriculum.start_year IS 'Intake year this plan applies to';
