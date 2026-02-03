-- Multiple roles per user: replace users.role with user_roles table

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT chk_user_role CHECK (role IN ('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'STAFF', 'STUDENT'))
);

-- Migrate existing single role from users to user_roles
INSERT INTO user_roles (user_id, role)
SELECT id, role FROM users;

-- Drop old column and constraint
ALTER TABLE users DROP CONSTRAINT IF EXISTS chk_role;
ALTER TABLE users DROP COLUMN role;

-- Index for finding users by role
CREATE INDEX idx_user_roles_role ON user_roles(role);

COMMENT ON TABLE user_roles IS 'User roles. A user may have multiple roles; at most one of STAFF, ADMIN, SUPER_ADMIN.';
