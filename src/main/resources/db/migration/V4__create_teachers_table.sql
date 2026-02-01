-- Teachers table for teacher-specific profile data
CREATE TABLE teachers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    
    teacher_id VARCHAR(50) NOT NULL UNIQUE,
    faculty VARCHAR(100) NOT NULL,
    english_name VARCHAR(100),
    position VARCHAR(100),
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_teachers_user_id ON teachers(user_id);
CREATE INDEX idx_teachers_teacher_id ON teachers(teacher_id);
CREATE INDEX idx_teachers_faculty ON teachers(faculty);

-- Comments
COMMENT ON TABLE teachers IS 'Teacher profile data linked to users with TEACHER role (OneToOne)';
COMMENT ON COLUMN teachers.user_id IS 'Reference to users table (must have role=TEACHER)';
COMMENT ON COLUMN teachers.teacher_id IS 'Unique teacher ID (personnel number) assigned by the university';
COMMENT ON COLUMN teachers.faculty IS 'Faculty/department the teacher belongs to';
COMMENT ON COLUMN teachers.english_name IS 'English name for international communication';
COMMENT ON COLUMN teachers.position IS 'Academic position (Professor, Associate Professor, Lecturer, etc.)';
