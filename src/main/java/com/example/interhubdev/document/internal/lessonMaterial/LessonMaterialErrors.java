package com.example.interhubdev.document.internal.lessonMaterial;

import com.example.interhubdev.error.AppException;
import com.example.interhubdev.error.Errors;
import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Lesson material module error codes and user-facing messages.
 * All errors are thrown as {@link AppException} and handled by global handler.
 */
public final class LessonMaterialErrors {

    private LessonMaterialErrors() {
    }

    /** Used when lesson material is not found by id. */
    public static final String CODE_MATERIAL_NOT_FOUND = "LESSON_MATERIAL_NOT_FOUND";
    /** Used when lesson is not found (e.g. invalid lessonId on create/list). */
    public static final String CODE_LESSON_NOT_FOUND = "LESSON_MATERIAL_LESSON_NOT_FOUND";
    /** Used when user tries to create material without required role (TEACHER/ADMIN). */
    public static final String CODE_CREATE_PERMISSION_DENIED = "LESSON_MATERIAL_CREATE_PERMISSION_DENIED";
    /** Used when user tries to delete or modify material they don't own and don't have admin role. */
    public static final String CODE_PERMISSION_DENIED = "LESSON_MATERIAL_PERMISSION_DENIED";
    /** Used when material name is empty or invalid. */
    public static final String CODE_INVALID_NAME = "LESSON_MATERIAL_INVALID_NAME";
    /** Used when an attachment is not found. */
    public static final String CODE_ATTACHMENT_NOT_FOUND = "LESSON_MATERIAL_ATTACHMENT_NOT_FOUND";
    /** Used when attachment is already linked to the material. */
    public static final String CODE_ATTACHMENT_ALREADY_IN_MATERIAL = "LESSON_MATERIAL_ATTACHMENT_ALREADY_IN_MATERIAL";
    /** Used when attachment link not found on removeFile. */
    public static final String CODE_ATTACHMENT_LINK_NOT_FOUND = "LESSON_MATERIAL_ATTACHMENT_LINK_NOT_FOUND";
    /** Used when DB save fails. */
    public static final String CODE_SAVE_FAILED = "LESSON_MATERIAL_SAVE_FAILED";

    public static AppException materialNotFound(UUID id) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_MATERIAL_NOT_FOUND, "Lesson material not found: " + id);
    }

    public static AppException lessonNotFound(UUID id) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_LESSON_NOT_FOUND, "Lesson not found: " + id);
    }

    public static AppException createPermissionDenied() {
        return Errors.forbidden("Only teachers and administrators can create lesson materials");
    }

    public static AppException permissionDenied() {
        return Errors.forbidden("You don't have permission to modify this lesson material");
    }

    public static AppException invalidName(String message) {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_INVALID_NAME, message);
    }

    public static AppException attachmentNotFound(UUID id) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_ATTACHMENT_NOT_FOUND, "Attachment not found: " + id);
    }

    public static AppException attachmentAlreadyInMaterial(UUID attachmentId) {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_ATTACHMENT_ALREADY_IN_MATERIAL,
            "Attachment already linked to this material: " + attachmentId);
    }

    public static AppException attachmentLinkNotFound(UUID materialId, UUID attachmentId) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_ATTACHMENT_LINK_NOT_FOUND,
            "Attachment is not linked to this material: " + materialId + ", attachment: " + attachmentId);
    }

    public static AppException saveFailed() {
        return Errors.of(HttpStatus.UNPROCESSABLE_ENTITY, CODE_SAVE_FAILED,
            "Failed to save lesson material. Please try again.");
    }
}
