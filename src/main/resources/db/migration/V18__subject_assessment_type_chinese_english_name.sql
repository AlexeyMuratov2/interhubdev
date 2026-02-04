-- =============================================================================
-- Subject and assessment_type: rename name -> chinese_name, add optional english_name
-- =============================================================================

-- Subject
ALTER TABLE subject RENAME COLUMN name TO chinese_name;
ALTER TABLE subject ADD COLUMN english_name VARCHAR(255) NULL;
COMMENT ON COLUMN subject.chinese_name IS 'Subject name in Chinese (required)';
COMMENT ON COLUMN subject.english_name IS 'Subject name in English (optional)';

-- Assessment type
ALTER TABLE assessment_type RENAME COLUMN name TO chinese_name;
ALTER TABLE assessment_type ADD COLUMN english_name VARCHAR(255) NULL;
COMMENT ON COLUMN assessment_type.chinese_name IS 'Assessment type name in Chinese (required)';
COMMENT ON COLUMN assessment_type.english_name IS 'Assessment type name in English (optional)';
