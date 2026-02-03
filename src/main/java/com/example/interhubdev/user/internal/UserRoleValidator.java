package com.example.interhubdev.user.internal;

import com.example.interhubdev.user.Role;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.util.Set;

/**
 * JPA entity listener that enforces at most one "staff-type" role (STAFF, MODERATOR, ADMIN, SUPER_ADMIN)
 * per user on every persist and update. Works together with the DB unique partial index
 * {@code idx_user_roles_one_staff_type_per_user}.
 */
final class UserRoleValidator {

    @PrePersist
    @PreUpdate
    void validateRoles(Object entity) {
        if (!(entity instanceof User user)) {
            return;
        }
        Set<Role> roles = user.getRoles();
        if (roles != null && !roles.isEmpty()) {
            Role.validateAtMostOneStaffType(roles);
        }
    }
}
