package com.example.interhubdev.submission.internal;

import com.example.interhubdev.error.AppException;
import com.example.interhubdev.error.Errors;
import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Submission module error codes and user-facing messages.
 * All errors are thrown as {@link AppException} and handled by global handler.
 */
public final class SubmissionErrors {

    private SubmissionErrors() {
    }

    /** Submission not found by id. */
    public static final String CODE_SUBMISSION_NOT_FOUND = "SUBMISSION_NOT_FOUND";
    /** Homework not found when creating or listing. */
    public static final String CODE_HOMEWORK_NOT_FOUND = "SUBMISSION_HOMEWORK_NOT_FOUND";
    /** Stored file not found when attaching to submission. */
    public static final String CODE_FILE_NOT_FOUND = "SUBMISSION_FILE_NOT_FOUND";
    /** User cannot perform action (role or not author). */
    public static final String CODE_PERMISSION_DENIED = "SUBMISSION_PERMISSION_DENIED";
    /** Description or other field validation failed. */
    public static final String CODE_VALIDATION_FAILED = "SUBMISSION_VALIDATION_FAILED";
    /** DB save failed. */
    public static final String CODE_SAVE_FAILED = "SUBMISSION_SAVE_FAILED";

    public static AppException submissionNotFound(UUID id) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_SUBMISSION_NOT_FOUND, "Submission not found: " + id);
    }

    public static AppException homeworkNotFound(UUID id) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_HOMEWORK_NOT_FOUND, "Homework not found: " + id);
    }

    public static AppException fileNotFound(UUID id) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_FILE_NOT_FOUND, "File not found: " + id);
    }

    public static AppException permissionDenied() {
        return Errors.forbidden("You don't have permission for this action");
    }

    public static AppException validationFailed(String message) {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_VALIDATION_FAILED, message);
    }

    public static AppException saveFailed() {
        return Errors.of(HttpStatus.UNPROCESSABLE_ENTITY, CODE_SAVE_FAILED,
            "Failed to save submission. Please try again.");
    }
}
