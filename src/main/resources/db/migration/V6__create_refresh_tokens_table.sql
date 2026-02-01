-- Refresh tokens table for JWT authentication
-- Stores refresh tokens to enable token revocation and session management

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMP,
    user_agent VARCHAR(500),
    ip_address VARCHAR(45)
);

-- Index for looking up tokens by user
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- Index for token lookup (used during refresh)
CREATE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);

-- Index for cleanup of expired/revoked tokens
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at) WHERE revoked = FALSE;

COMMENT ON TABLE refresh_tokens IS 'Stores refresh tokens for JWT authentication with revocation support';
COMMENT ON COLUMN refresh_tokens.token_hash IS 'SHA-256 hash of the refresh token (never store plain token)';
COMMENT ON COLUMN refresh_tokens.user_agent IS 'Browser/client user agent for session identification';
COMMENT ON COLUMN refresh_tokens.ip_address IS 'Client IP address (IPv4 or IPv6)';
