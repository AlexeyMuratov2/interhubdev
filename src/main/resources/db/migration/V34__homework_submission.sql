-- Homework submissions: student solutions for a homework assignment.
-- Files are optional (student can submit description only or attach multiple files).
CREATE TABLE homework_submission (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    homework_id UUID NOT NULL REFERENCES homework(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    submitted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    description TEXT
);

CREATE INDEX idx_homework_submission_homework_id ON homework_submission(homework_id);
CREATE INDEX idx_homework_submission_author_id ON homework_submission(author_id);

COMMENT ON TABLE homework_submission IS 'Student submissions for homework assignments. Author is the student; files are optional.';
COMMENT ON COLUMN homework_submission.homework_id IS 'FK to homework (assignment)';
COMMENT ON COLUMN homework_submission.author_id IS 'Student who submitted (FK to users)';
COMMENT ON COLUMN homework_submission.submitted_at IS 'When the submission was created';
COMMENT ON COLUMN homework_submission.description IS 'Optional text description from the student';

-- Multiple files per submission (optional; can be zero).
CREATE TABLE homework_submission_file (
    submission_id UUID NOT NULL REFERENCES homework_submission(id) ON DELETE CASCADE,
    stored_file_id UUID NOT NULL REFERENCES stored_file(id) ON DELETE RESTRICT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (submission_id, stored_file_id)
);

CREATE INDEX idx_homework_submission_file_stored_file_id ON homework_submission_file(stored_file_id);

COMMENT ON TABLE homework_submission_file IS 'Attached files for a submission. Files are optional; submission can have zero or more.';
COMMENT ON COLUMN homework_submission_file.sort_order IS 'Display order of files';
