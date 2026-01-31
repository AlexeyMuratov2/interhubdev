-- Users table for authentication
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    role VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    activated_at TIMESTAMP,
    
    CONSTRAINT chk_role CHECK (role IN ('SUPER_ADMIN', 'ADMIN', 'TEACHER', 'STAFF', 'STUDENT')),
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'ACTIVE', 'DISABLED'))
);

-- Index for email lookups (login)
CREATE INDEX idx_users_email ON users(email);

-- Index for role-based queries
CREATE INDEX idx_users_role ON users(role);

-- Index for status-based queries
CREATE INDEX idx_users_status ON users(status);

COMMENT ON TABLE users IS 'Core user entity for authentication. Role-specific data stored in profile tables.';
COMMENT ON COLUMN users.password_hash IS 'BCrypt encoded password. NULL until user activates account.';
COMMENT ON COLUMN users.status IS 'PENDING - invited but not activated, ACTIVE - can login, DISABLED - blocked by admin';
COMMENT ON COLUMN users.role IS 'SUPER_ADMIN - full access, can invite admins. ADMIN - manage users except admins. TEACHER - professors. STAFF - employees. STUDENT - international students.';
