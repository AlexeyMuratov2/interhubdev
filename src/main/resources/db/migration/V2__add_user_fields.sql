-- Add new fields to users table
ALTER TABLE users ADD COLUMN phone VARCHAR(20);
ALTER TABLE users ADD COLUMN birth_date DATE;
ALTER TABLE users ADD COLUMN last_login_at TIMESTAMP;

COMMENT ON COLUMN users.phone IS 'User phone number';
COMMENT ON COLUMN users.birth_date IS 'User date of birth';
COMMENT ON COLUMN users.last_login_at IS 'Timestamp of last successful login';
