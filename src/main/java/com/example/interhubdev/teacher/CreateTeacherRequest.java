package com.example.interhubdev.teacher;

/**
 * Request object for creating a teacher profile.
 * Used when inviting a new teacher or creating profile manually.
 */
public record CreateTeacherRequest(
    String teacherId,
    String faculty,
    String englishName,
    String position
) {
    /**
     * Creates a builder for fluent construction.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String teacherId;
        private String faculty;
        private String englishName;
        private String position;

        public Builder teacherId(String teacherId) {
            this.teacherId = teacherId;
            return this;
        }

        public Builder faculty(String faculty) {
            this.faculty = faculty;
            return this;
        }

        public Builder englishName(String englishName) {
            this.englishName = englishName;
            return this;
        }

        public Builder position(String position) {
            this.position = position;
            return this;
        }

        public CreateTeacherRequest build() {
            if (teacherId == null || teacherId.isBlank()) {
                throw new IllegalArgumentException("teacherId is required");
            }
            if (faculty == null || faculty.isBlank()) {
                throw new IllegalArgumentException("faculty is required");
            }
            return new CreateTeacherRequest(teacherId, faculty, englishName, position);
        }
    }
}
