package com.example.interhubdev.storedfile.internal;

import com.example.interhubdev.error.AppException;
import com.example.interhubdev.error.Errors;
import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Storedfile module error codes and factories.
 */
public final class StoredFileErrors {

    private StoredFileErrors() {
    }

    public static final String CODE_STORED_FILE_NOT_FOUND = "STORED_FILE_NOT_FOUND";
    public static final String CODE_FILE_IN_USE = "STORED_FILE_IN_USE";
    public static final String CODE_FILE_NOT_IN_STORAGE = "STORED_FILE_NOT_IN_STORAGE";
    public static final String CODE_SAVE_FAILED = "STORED_FILE_SAVE_FAILED";
    public static final String CODE_BATCH_TOO_LARGE = "STORED_FILE_BATCH_TOO_LARGE";
    public static final String CODE_STORAGE_UNAVAILABLE = "STORED_FILE_STORAGE_UNAVAILABLE";
    public static final String CODE_UPLOAD_FAILED = "STORED_FILE_UPLOAD_FAILED";
    public static final String CODE_INVALID_FILE = "STORED_FILE_INVALID_FILE";
    /** File not yet ACTIVE; bind/download forbidden until activation gate passed. */
    public static final String CODE_FILE_NOT_ACTIVE = "STORED_FILE_NOT_ACTIVE";
    /** Delivery not allowed for this file's safety class and requested context. */
    public static final String CODE_DELIVERY_NOT_ALLOWED = "STORED_FILE_DELIVERY_NOT_ALLOWED";

    public static AppException fileNotActive() {
        return Errors.of(HttpStatus.CONFLICT, CODE_FILE_NOT_ACTIVE, "File is not available for download (activation gate not passed).");
    }

    public static AppException deliveryNotAllowed() {
        return Errors.of(HttpStatus.FORBIDDEN, CODE_DELIVERY_NOT_ALLOWED, "Delivery not allowed for this file in the requested context.");
    }

    public static AppException invalidFile(String message) {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_INVALID_FILE, message);
    }

    public static AppException storedFileNotFound(UUID id) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_STORED_FILE_NOT_FOUND, "Stored file not found: " + id);
    }

    public static AppException fileInUse() {
        return Errors.of(HttpStatus.CONFLICT, CODE_FILE_IN_USE, "Cannot delete file: file is currently in use");
    }

    public static AppException fileNotFoundInStorage() {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_FILE_NOT_IN_STORAGE, "File not found in storage.");
    }

    public static AppException saveFailed() {
        return Errors.of(HttpStatus.INTERNAL_SERVER_ERROR, CODE_SAVE_FAILED, "Failed to save file metadata.");
    }

    public static AppException batchTooLarge(int maxFiles) {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_BATCH_TOO_LARGE, "Too many files in batch. Maximum: " + maxFiles);
    }

    public static AppException storageUnavailable() {
        return Errors.of(HttpStatus.SERVICE_UNAVAILABLE, CODE_STORAGE_UNAVAILABLE, "Storage service is temporarily unavailable.");
    }

    public static AppException uploadFailed() {
        return Errors.of(HttpStatus.INTERNAL_SERVER_ERROR, CODE_UPLOAD_FAILED, "Failed to upload file.");
    }
}
