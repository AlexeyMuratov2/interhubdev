package com.example.interhubdev.document.internal.homework;

import com.example.interhubdev.error.AppException;
import com.example.interhubdev.error.Errors;
import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Homework module error codes and user-facing messages for frontend.
 * All errors are thrown as {@link AppException} and handled by global handler.
 */
public final class HomeworkErrors {

    private HomeworkErrors() {
    }

    /** Homework not found by id. */
    public static final String CODE_HOMEWORK_NOT_FOUND = "HOMEWORK_NOT_FOUND";
    /** Lesson not found when creating or listing. */
    public static final String CODE_LESSON_NOT_FOUND = "HOMEWORK_LESSON_NOT_FOUND";
    /** Stored file not found when attaching to homework. */
    public static final String CODE_FILE_NOT_FOUND = "HOMEWORK_FILE_NOT_FOUND";
    /** User cannot create/edit/delete homework (role). */
    public static final String CODE_PERMISSION_DENIED = "HOMEWORK_PERMISSION_DENIED";
    /** Title or other field validation failed. */
    public static final String CODE_VALIDATION_FAILED = "HOMEWORK_VALIDATION_FAILED";
    /** DB save failed (e.g. constraint). */
    public static final String CODE_SAVE_FAILED = "HOMEWORK_SAVE_FAILED";

    public static AppException homeworkNotFound(UUID id) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_HOMEWORK_NOT_FOUND, "Homework not found: " + id);
    }

    public static AppException lessonNotFound(UUID lessonId) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_LESSON_NOT_FOUND, "Lesson not found: " + lessonId);
    }

    public static AppException fileNotFound(UUID fileId) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_FILE_NOT_FOUND, "File not found: " + fileId);
    }

    public static AppException permissionDenied() {
        return Errors.forbidden("You don't have permission to manage homework");
    }

    public static AppException validationFailed(String message) {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_VALIDATION_FAILED, message);
    }

    public static AppException saveFailed() {
        return Errors.of(HttpStatus.UNPROCESSABLE_ENTITY, CODE_SAVE_FAILED,
            "Failed to save homework. Please try again.");
    }
}
