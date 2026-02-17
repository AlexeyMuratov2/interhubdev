package com.example.interhubdev.document.internal.courseMaterial;

import com.example.interhubdev.error.AppException;
import com.example.interhubdev.error.Errors;
import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Course material module error codes and user-facing messages.
 * All errors are thrown as {@link AppException} and handled by global handler.
 */
public final class CourseMaterialErrors {

    private CourseMaterialErrors() {
    }

    /** Used when course material is not found by id. */
    public static final String CODE_MATERIAL_NOT_FOUND = "COURSE_MATERIAL_NOT_FOUND";
    /** Used when user tries to create material without required role (TEACHER/ADMIN). */
    public static final String CODE_CREATE_PERMISSION_DENIED = "COURSE_MATERIAL_CREATE_PERMISSION_DENIED";
    /** Used when user tries to delete material they don't own and don't have admin role. */
    public static final String CODE_DELETE_PERMISSION_DENIED = "COURSE_MATERIAL_DELETE_PERMISSION_DENIED";
    /** Used when material title is empty or invalid. */
    public static final String CODE_INVALID_TITLE = "COURSE_MATERIAL_INVALID_TITLE";
    /** Used when course material with the same file already exists for the subject. */
    public static final String CODE_MATERIAL_ALREADY_EXISTS = "COURSE_MATERIAL_ALREADY_EXISTS";
    /** Used when DB save fails (e.g. subject not found, constraint violation). */
    public static final String CODE_SAVE_FAILED = "COURSE_MATERIAL_SAVE_FAILED";

    public static AppException materialNotFound(UUID id) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_MATERIAL_NOT_FOUND, "Course material not found: " + id);
    }

    public static AppException createPermissionDenied() {
        return Errors.forbidden("Only teachers and administrators can create course materials");
    }

    public static AppException deletePermissionDenied() {
        return Errors.forbidden("You don't have permission to delete this course material");
    }

    public static AppException invalidTitle(String message) {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_INVALID_TITLE, message);
    }

    public static AppException materialAlreadyExists(UUID subjectId) {
        return Errors.of(HttpStatus.CONFLICT, CODE_MATERIAL_ALREADY_EXISTS,
            "Course material with this file already exists for subject: " + subjectId);
    }

    public static AppException saveFailed() {
        return Errors.of(HttpStatus.UNPROCESSABLE_ENTITY, CODE_SAVE_FAILED,
            "Failed to save course material. Please check that the subject exists and try again.");
    }
}
