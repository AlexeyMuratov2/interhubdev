package com.example.interhubdev.user;

import java.util.Set;

/**
 * User roles in the system.
 * Each role has different access levels and associated profile data.
 * <p>
 * A user may have multiple roles. At most one "staff-type" role is allowed per user:
 * STAFF, ADMIN, or SUPER_ADMIN. Other roles (TEACHER, STUDENT) may be combined freely.
 */
public enum Role {
    /**
     * Super administrator - can invite other admins and has full system access.
     */
    SUPER_ADMIN,

    /**
     * Regular administrator - can manage users (except admins), content, etc.
     */
    ADMIN,

    /**
     * Teacher/Professor - can manage course materials, grades, etc.
     */
    TEACHER,

    /**
     * Staff member - university employees (not teachers).
     */
    STAFF,

    /**
     * Student - international student user.
     */
    STUDENT;

    /**
     * Check if this role can invite users with the target role.
     */
    public boolean canInvite(Role targetRole) {
        return switch (this) {
            case SUPER_ADMIN -> true; // can invite anyone
            case ADMIN -> targetRole != SUPER_ADMIN && targetRole != ADMIN; // cannot invite admins
            default -> false; // others cannot invite
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
    public static final Set<Role> STAFF_TYPE_ROLES = Set.of(STAFF, ADMIN, SUPER_ADMIN);

    /**
     * True if this role is STAFF, ADMIN, or SUPER_ADMIN (at most one per user).
     */
    public boolean isStaffType() {
        return STAFF_TYPE_ROLES.contains(this);
    }

    /**
     * Validates that the user has at most one staff-type role (STAFF, ADMIN, SUPER_ADMIN).
     *
     * @throws IllegalArgumentException if more than one staff-type role is present
     */
    public static void validateAtMostOneStaffType(Set<Role> roles) {
        if (roles == null) return;
        long staffTypeCount = roles.stream().filter(Role::isStaffType).count();
        if (staffTypeCount > 1) {
            throw new IllegalArgumentException(
                    "User may have at most one role from [STAFF, ADMIN, SUPER_ADMIN]. Found: " + roles);
        }
    }
}
