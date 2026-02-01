-- Students table for student-specific profile data
CREATE TABLE students (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    
    student_id VARCHAR(50) NOT NULL UNIQUE,
    chinese_name VARCHAR(100),
    faculty VARCHAR(100) NOT NULL,
    course VARCHAR(100),
    enrollment_year INTEGER,
    group_name VARCHAR(50),
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_students_user_id ON students(user_id);
CREATE INDEX idx_students_student_id ON students(student_id);
CREATE INDEX idx_students_faculty ON students(faculty);
CREATE INDEX idx_students_group_name ON students(group_name);

-- Comments
COMMENT ON TABLE students IS 'Student profile data linked to users with STUDENT role (OneToOne)';
COMMENT ON COLUMN students.user_id IS 'Reference to users table (must have role=STUDENT)';
COMMENT ON COLUMN students.student_id IS 'Unique student ID assigned by the university';
COMMENT ON COLUMN students.chinese_name IS 'Chinese name for international students from China';
COMMENT ON COLUMN students.faculty IS 'Faculty/department the student belongs to';
COMMENT ON COLUMN students.course IS 'Course/program of study';
COMMENT ON COLUMN students.enrollment_year IS 'Year when the student enrolled';
COMMENT ON COLUMN students.group_name IS 'Academic group';
