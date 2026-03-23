package com.example.interhubdev.storedfile.internal.uploadSecurity;

import com.example.interhubdev.error.AppException;
import com.example.interhubdev.error.Errors;
import org.springframework.http.HttpStatus;

/**
 * Error codes and factories for upload security layer.
 */
public final class UploadSecurityErrors {

    private UploadSecurityErrors() {
    }

    public static final String CODE_EMPTY_FILE = "UPLOAD_EMPTY_FILE";
    public static final String CODE_FILE_TOO_LARGE = "UPLOAD_FILE_TOO_LARGE";
    public static final String CODE_FORBIDDEN_FILE_TYPE = "UPLOAD_FORBIDDEN_FILE_TYPE";
    public static final String CODE_SUSPICIOUS_FILENAME = "UPLOAD_SUSPICIOUS_FILENAME";
    public static final String CODE_CONTENT_TYPE_MISMATCH = "UPLOAD_CONTENT_TYPE_MISMATCH";
    public static final String CODE_MALWARE_DETECTED = "UPLOAD_MALWARE_DETECTED";
    public static final String CODE_AV_UNAVAILABLE = "UPLOAD_AV_UNAVAILABLE";
    public static final String CODE_CAPACITY_MISMATCH = "UPLOAD_CAPACITY_MISMATCH";

    public static AppException emptyFile(String message) {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_EMPTY_FILE, message);
    }

    public static AppException fileTooLarge(long maxBytes) {
        return Errors.of(HttpStatus.PAYLOAD_TOO_LARGE, CODE_FILE_TOO_LARGE,
            "File size exceeds maximum allowed size of " + (maxBytes / (1024 * 1024)) + " MB");
    }

    public static AppException capacityMismatch(long requestedBytes, long effectiveMaxBytes) {
        return Errors.of(HttpStatus.PAYLOAD_TOO_LARGE, CODE_CAPACITY_MISMATCH,
            "File size " + requestedBytes + " exceeds current secure processing capacity of " + effectiveMaxBytes + " bytes.");
    }

    public static AppException forbiddenFileType(String message) {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_FORBIDDEN_FILE_TYPE, message);
    }

    public static AppException suspiciousFilename(String message) {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_SUSPICIOUS_FILENAME, message);
    }

    public static AppException contentTypeMismatch(String message) {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_CONTENT_TYPE_MISMATCH, message);
    }

    public static AppException malwareDetected() {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_MALWARE_DETECTED, "File rejected");
    }

    public static AppException avUnavailable(String message) {
        return Errors.of(HttpStatus.SERVICE_UNAVAILABLE, CODE_AV_UNAVAILABLE, message);
    }
}
