-- Add MODERATOR role to allowed user roles

ALTER TABLE user_roles DROP CONSTRAINT chk_user_role;

ALTER TABLE user_roles ADD CONSTRAINT chk_user_role CHECK (
    role IN ('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'TEACHER', 'STAFF', 'STUDENT')
);

COMMENT ON TABLE user_roles IS 'User roles. A user may have multiple roles; at most one of STAFF, MODERATOR, ADMIN, SUPER_ADMIN. STAFF is read-only; MODERATOR can edit all except invitations.';
