package com.example.interhubdev.invitation;

import com.example.interhubdev.student.CreateStudentRequest;
import com.example.interhubdev.teacher.CreateTeacherRequest;
import com.example.interhubdev.user.Role;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Request object for creating an invitation.
 * Contains user data and role-specific profile data.
 * Accepts either single {@code role} or multiple {@code roles}; at most one of STAFF, ADMIN, SUPER_ADMIN.
 */
public record CreateInvitationRequest(
    String email,
    Role role,
    List<Role> roles,
    String firstName,
    String lastName,
    String phone,
    LocalDate birthDate,
    CreateStudentRequest studentData,
    CreateTeacherRequest teacherData
) {
    /**
     * Effective roles: from {@code roles} if present, otherwise from single {@code role}.
     * Never null; empty if neither role nor roles provided.
     */
    public Set<Role> getEffectiveRoles() {
        if (roles != null && !roles.isEmpty()) {
            return Set.copyOf(roles);
        }
        if (role != null) {
            return Set.of(role);
        }
        return Set.of();
    }

    /**
     * Primary role for backward compatibility (e.g. profile validation).
     * Returns first effective role, or null if none.
     */
    public Role getPrimaryRole() {
        Set<Role> effective = getEffectiveRoles();
        return effective.isEmpty() ? null : effective.iterator().next();
    }
    /**
     * Creates a builder for fluent construction.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String email;
        private Role role;
        private List<Role> roles;
        private String firstName;
        private String lastName;
        private String phone;
        private LocalDate birthDate;
        private CreateStudentRequest studentData;
        private CreateTeacherRequest teacherData;

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder role(Role role) {
            this.role = role;
            return this;
        }

        public Builder roles(List<Role> roles) {
            this.roles = roles;
            return this;
        }

        public Builder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder birthDate(LocalDate birthDate) {
            this.birthDate = birthDate;
            return this;
        }

        public Builder studentData(CreateStudentRequest studentData) {
            this.studentData = studentData;
            return this;
        }

        public Builder teacherData(CreateTeacherRequest teacherData) {
            this.teacherData = teacherData;
            return this;
        }

        public CreateInvitationRequest build() {
            if (email == null || email.isBlank()) {
                throw new IllegalArgumentException("email is required");
            }
            Set<Role> effective = (roles != null && !roles.isEmpty())
                ? Set.copyOf(roles)
                : (role != null ? Set.of(role) : Set.of());
            if (effective.isEmpty()) {
                throw new IllegalArgumentException("role or roles is required");
            }
            Role.validateAtMostOneStaffType(effective);

            // Validate role-specific data
            if (effective.contains(Role.STUDENT) && studentData == null) {
                throw new IllegalArgumentException("studentData is required when inviting as STUDENT");
            }
            if (effective.contains(Role.TEACHER) && teacherData == null) {
                throw new IllegalArgumentException("teacherData is required when inviting as TEACHER");
            }

            return new CreateInvitationRequest(
                email, role, roles != null ? List.copyOf(roles) : null, firstName, lastName, phone, birthDate, studentData, teacherData
            );
        }
    }
}
