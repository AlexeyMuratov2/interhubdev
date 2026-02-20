package com.example.interhubdev.grades.internal;

import com.example.interhubdev.error.AppException;
import com.example.interhubdev.error.Errors;
import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Grades module error codes and user-facing messages.
 * All errors are thrown as {@link AppException} and handled by global handler.
 */
public final class GradeErrors {

    private GradeErrors() {
    }

    public static final String CODE_ENTRY_NOT_FOUND = "GRADE_ENTRY_NOT_FOUND";
    public static final String CODE_OFFERING_NOT_FOUND = "GRADE_OFFERING_NOT_FOUND";
    public static final String CODE_STUDENT_NOT_FOUND = "GRADE_STUDENT_NOT_FOUND";
    public static final String CODE_GROUP_NOT_FOUND = "GRADE_GROUP_NOT_FOUND";
    public static final String CODE_OFFERING_NOT_FOR_GROUP = "GRADE_OFFERING_NOT_FOR_GROUP";
    public static final String CODE_VALIDATION_FAILED = "GRADE_VALIDATION_FAILED";
    public static final String CODE_ENTRY_VOIDED = "GRADE_ENTRY_VOIDED";
    public static final String CODE_FORBIDDEN = "GRADE_FORBIDDEN";
    public static final String CODE_LESSON_NOT_FOUND = "GRADE_LESSON_NOT_FOUND";
    public static final String CODE_STUDENT_NOT_IN_GROUP = "GRADE_STUDENT_NOT_IN_GROUP";

    public static AppException lessonNotFound(UUID id) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_LESSON_NOT_FOUND, "Lesson not found: " + id);
    }

    public static AppException studentNotInGroup(UUID studentId, UUID groupId) {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_STUDENT_NOT_IN_GROUP,
                "Student " + studentId + " is not in group " + groupId);
    }

    public static AppException entryNotFound(UUID id) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_ENTRY_NOT_FOUND, "Grade entry not found: " + id);
    }

    public static AppException offeringNotFound(UUID id) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_OFFERING_NOT_FOUND, "Offering not found: " + id);
    }

    public static AppException studentNotFound(UUID id) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_STUDENT_NOT_FOUND, "Student not found: " + id);
    }

    public static AppException groupNotFound(UUID id) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_GROUP_NOT_FOUND, "Group not found: " + id);
    }

    public static AppException offeringNotForGroup(UUID offeringId, UUID groupId) {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_OFFERING_NOT_FOR_GROUP,
                "Offering " + offeringId + " does not belong to group " + groupId);
    }

    public static AppException validationFailed(String message) {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_VALIDATION_FAILED, message);
    }

    public static AppException entryVoided(UUID id) {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_ENTRY_VOIDED,
                "Cannot update voided grade entry: " + id);
    }

    public static AppException forbidden() {
        return Errors.of(HttpStatus.FORBIDDEN, CODE_FORBIDDEN,
                "Only teachers or administrators can manage grades");
    }
}
