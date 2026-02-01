package com.example.interhubdev.invitation;

import com.example.interhubdev.student.CreateStudentRequest;
import com.example.interhubdev.teacher.CreateTeacherRequest;
import com.example.interhubdev.user.Role;

import java.time.LocalDate;

/**
 * Request object for creating an invitation.
 * Contains user data and role-specific profile data.
 */
public record CreateInvitationRequest(
    String email,
    Role role,
    String firstName,
    String lastName,
    String phone,
    LocalDate birthDate,
    CreateStudentRequest studentData,
    CreateTeacherRequest teacherData
) {
    /**
     * Creates a builder for fluent construction.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String email;
        private Role role;
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
            if (role == null) {
                throw new IllegalArgumentException("role is required");
            }
            
            // Validate role-specific data
            if (role == Role.STUDENT && studentData == null) {
                throw new IllegalArgumentException("studentData is required for STUDENT role");
            }
            if (role == Role.TEACHER && teacherData == null) {
                throw new IllegalArgumentException("teacherData is required for TEACHER role");
            }
            
            return new CreateInvitationRequest(
                email, role, firstName, lastName, phone, birthDate, studentData, teacherData
            );
        }
    }
}
