-- Enforce at most one of STAFF, MODERATOR, ADMIN, SUPER_ADMIN per user.
-- A user may have multiple roles (e.g. STUDENT + TEACHER) but only one "staff-type" role.

CREATE UNIQUE INDEX idx_user_roles_one_staff_type_per_user ON user_roles (user_id)
WHERE role IN ('STAFF', 'MODERATOR', 'ADMIN', 'SUPER_ADMIN');

COMMENT ON INDEX idx_user_roles_one_staff_type_per_user IS 'Ensures each user has at most one of STAFF, MODERATOR, ADMIN, SUPER_ADMIN.';
