-- Invitations table
CREATE TABLE invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    invited_by_id UUID REFERENCES users(id),
    
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    
    email_sent_at TIMESTAMP,
    email_message_id VARCHAR(255),
    email_attempts INTEGER DEFAULT 0,
    
    expires_at TIMESTAMP NOT NULL,
    accepted_at TIMESTAMP,
    
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_invitation_status CHECK (status IN 
        ('PENDING', 'SENDING', 'SENT', 'FAILED', 'ACCEPTED', 'EXPIRED', 'CANCELLED'))
);

-- Invitation tokens table (short-lived tokens for email links)
CREATE TABLE invitation_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invitation_id UUID NOT NULL REFERENCES invitations(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for invitations
CREATE INDEX idx_invitations_user_id ON invitations(user_id);
CREATE INDEX idx_invitations_invited_by_id ON invitations(invited_by_id);
CREATE INDEX idx_invitations_status ON invitations(status);
CREATE INDEX idx_invitations_expires_at ON invitations(expires_at);

-- Indexes for invitation_tokens
CREATE INDEX idx_invitation_tokens_invitation_id ON invitation_tokens(invitation_id);
CREATE INDEX idx_invitation_tokens_token ON invitation_tokens(token);
CREATE INDEX idx_invitation_tokens_expires_at ON invitation_tokens(expires_at);

-- Comments
COMMENT ON TABLE invitations IS 'User invitations. Validity: 3 months. Status tracks email sending and acceptance.';
COMMENT ON COLUMN invitations.user_id IS 'The user being invited (created with PENDING status)';
COMMENT ON COLUMN invitations.invited_by_id IS 'Admin who created the invitation';
COMMENT ON COLUMN invitations.email_attempts IS 'Number of email send attempts (max 3)';
COMMENT ON COLUMN invitations.expires_at IS 'When the invitation expires (3 months from creation)';

COMMENT ON TABLE invitation_tokens IS 'Short-lived tokens for invitation email links. Validity: 24 hours. Auto-regenerated if expired but invitation valid.';
COMMENT ON COLUMN invitation_tokens.token IS 'Random token string included in invitation email link';
COMMENT ON COLUMN invitation_tokens.expires_at IS 'When the token expires (24 hours from creation)';
