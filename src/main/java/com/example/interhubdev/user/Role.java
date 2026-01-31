package com.example.interhubdev.user;

/**
 * User roles in the system.
 * Each role has different access levels and associated profile data.
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
}
