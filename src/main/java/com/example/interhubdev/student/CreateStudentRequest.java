package com.example.interhubdev.student;

/**
 * Request object for creating a student profile.
 * Used when inviting a new student or creating profile manually.
 */
public record CreateStudentRequest(
    String studentId,
    String chineseName,
    String faculty,
    String course,
    Integer enrollmentYear,
    String groupName
) {
    /**
     * Creates a builder for fluent construction.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String studentId;
        private String chineseName;
        private String faculty;
        private String course;
        private Integer enrollmentYear;
        private String groupName;

        public Builder studentId(String studentId) {
            this.studentId = studentId;
            return this;
        }

        public Builder chineseName(String chineseName) {
            this.chineseName = chineseName;
            return this;
        }

        public Builder faculty(String faculty) {
            this.faculty = faculty;
            return this;
        }

        public Builder course(String course) {
            this.course = course;
            return this;
        }

        public Builder enrollmentYear(Integer enrollmentYear) {
            this.enrollmentYear = enrollmentYear;
            return this;
        }

        public Builder groupName(String groupName) {
            this.groupName = groupName;
            return this;
        }

        public CreateStudentRequest build() {
            if (studentId == null || studentId.isBlank()) {
                throw new IllegalArgumentException("studentId is required");
            }
            if (faculty == null || faculty.isBlank()) {
                throw new IllegalArgumentException("faculty is required");
            }
            return new CreateStudentRequest(studentId, chineseName, faculty, course, enrollmentYear, groupName);
        }
    }
}
