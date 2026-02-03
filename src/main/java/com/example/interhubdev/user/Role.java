package com.example.interhubdev.user;

import java.util.Set;

/**
 * User roles in the system.
 * Each role has different access levels and associated profile data.
 * <p>
 * A user may have multiple roles. At most one "staff-type" role is allowed per user:
 * STAFF, MODERATOR, ADMIN, or SUPER_ADMIN. Other roles (TEACHER, STUDENT) may be combined freely.
 */
public enum Role {
    /**
     * Super administrator - can invite other admins and has full system access.
     */
    SUPER_ADMIN,

    /**
     * Regular administrator - can manage users (except admins), content, and invite new users.
     */
    ADMIN,

    /**
     * Moderator - can create/update/delete all content except invitations (read-only for invitations).
     */
    MODERATOR,

    /**
     * Teacher/Professor - can manage course materials, grades, etc.
     */
    TEACHER,

    /**
     * Staff member - read-only access to catalog data; cannot create, update or delete.
     */
    STAFF,

    /**
     * Student - international student user.
     */
    STUDENT;

    /**
     * Check if this role can invite users with the target role.
     * Only SUPER_ADMIN and ADMIN can invite; MODERATOR and STAFF cannot.
     */
    public boolean canInvite(Role targetRole) {
        return switch (this) {
            case SUPER_ADMIN -> true; // can invite anyone
            case ADMIN -> targetRole != SUPER_ADMIN && targetRole != ADMIN; // cannot invite admins
            default -> false; // MODERATOR, STAFF, TEACHER, STUDENT cannot invite
        };
    }

    /**
     * Check if this role has admin privileges.
     */
    public boolean isAdmin() {
        return this == SUPER_ADMIN || this == ADMIN;
    }

    /**
     * Roles that are mutually exclusive: a user may have at most one of these.
     */
    public static final Set<Role> STAFF_TYPE_ROLES = Set.of(STAFF, MODERATOR, ADMIN, SUPER_ADMIN);

    /**
     * True if this role is STAFF, MODERATOR, ADMIN, or SUPER_ADMIN (at most one per user).
     */
    public boolean isStaffType() {
        return STAFF_TYPE_ROLES.contains(this);
    }

    /**
     * Validates that the user has at most one staff-type role (STAFF, MODERATOR, ADMIN, SUPER_ADMIN).
     * Enforced also at DB level by unique partial index on user_roles.
     *
     * @param roles user roles (may be null or empty)
     * @throws IllegalArgumentException if more than one staff-type role is present
     */
    public static void validateAtMostOneStaffType(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) return;
        long staffTypeCount = roles.stream().filter(Role::isStaffType).count();
        if (staffTypeCount > 1) {
            throw new IllegalArgumentException(
                    "User may have at most one role from [STAFF, MODERATOR, ADMIN, SUPER_ADMIN]. Found: " + roles);
        }
    }
}
