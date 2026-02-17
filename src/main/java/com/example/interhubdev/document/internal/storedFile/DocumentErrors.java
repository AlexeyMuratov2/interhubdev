package com.example.interhubdev.document.internal.storedFile;

import com.example.interhubdev.error.AppException;
import com.example.interhubdev.error.Errors;
import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Document module error codes and user-facing messages.
 * All errors are thrown as {@link AppException} and handled by global handler.
 */
public final class DocumentErrors {

    private DocumentErrors() {
    }

    /** Used when stored file is not found by id. */
    public static final String CODE_STORED_FILE_NOT_FOUND = "STORED_FILE_NOT_FOUND";
    /** Used when file size exceeds allowed maximum. */
    public static final String CODE_FILE_TOO_LARGE = "FILE_TOO_LARGE";
    /** Used when file type (MIME or extension) is not allowed. */
    public static final String CODE_INVALID_FILE_TYPE = "INVALID_FILE_TYPE";
    /** Used when file name is empty or invalid. */
    public static final String CODE_INVALID_FILE_NAME = "INVALID_FILE_NAME";
    /** Used when storage is unavailable (network error, timeout, service down). */
    public static final String CODE_STORAGE_UNAVAILABLE = "STORAGE_UNAVAILABLE";
    /** Used when file upload fails (generic storage error). */
    public static final String CODE_UPLOAD_FAILED = "UPLOAD_FAILED";
    /** Used when user tries to access file they don't have permission for. */
    public static final String CODE_ACCESS_DENIED = "ACCESS_DENIED";
    /** Used when trying to delete a file that is in use (referenced by Document). */
    public static final String CODE_FILE_IN_USE = "FILE_IN_USE";

    public static AppException storedFileNotFound(UUID id) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_STORED_FILE_NOT_FOUND, "Stored file not found: " + id);
    }

    public static AppException fileTooLarge(long maxBytes) {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_FILE_TOO_LARGE,
            "File size exceeds maximum allowed size of " + (maxBytes / (1024 * 1024)) + " MB");
    }

    public static AppException invalidFileType(String message) {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_INVALID_FILE_TYPE, message);
    }

    public static AppException invalidFileName(String message) {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_INVALID_FILE_NAME, message);
    }

    public static AppException storageUnavailable() {
        return Errors.of(HttpStatus.SERVICE_UNAVAILABLE, CODE_STORAGE_UNAVAILABLE,
            "Storage service is temporarily unavailable. Please try again later.");
    }

    public static AppException uploadFailed() {
        return Errors.of(HttpStatus.INTERNAL_SERVER_ERROR, CODE_UPLOAD_FAILED,
            "Failed to upload file. Please try again.");
    }

    public static AppException accessDenied() {
        return Errors.of(HttpStatus.FORBIDDEN, CODE_ACCESS_DENIED, "You don't have permission to access this file");
    }

    public static AppException fileInUse() {
        return Errors.of(HttpStatus.CONFLICT, CODE_FILE_IN_USE, "Cannot delete file: file is currently in use");
    }
}
