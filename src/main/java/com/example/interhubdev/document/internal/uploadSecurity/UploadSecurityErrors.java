package com.example.interhubdev.document.internal.uploadSecurity;

import com.example.interhubdev.error.AppException;
import com.example.interhubdev.error.Errors;
import org.springframework.http.HttpStatus;

/**
 * Error codes and factories for the upload security layer.
 * All errors are thrown as {@link AppException} and handled by the global handler.
 */
public final class UploadSecurityErrors {

    private UploadSecurityErrors() {
    }

    /** Content type or extension is not allowed by policy. */
    public static final String CODE_FORBIDDEN_FILE_TYPE = "UPLOAD_FORBIDDEN_FILE_TYPE";
    /** Filename suggests malicious content (double extension, path traversal, etc.). */
    public static final String CODE_SUSPICIOUS_FILENAME = "UPLOAD_SUSPICIOUS_FILENAME";
    /** File extension does not match declared content type. */
    public static final String CODE_EXTENSION_MISMATCH = "UPLOAD_EXTENSION_MISMATCH";
    /** File size exceeds allowed maximum. */
    public static final String CODE_FILE_TOO_LARGE = "UPLOAD_FILE_TOO_LARGE";
    /** Magic bytes (content signature) contradict declared MIME/extension. */
    public static final String CODE_CONTENT_TYPE_MISMATCH = "UPLOAD_CONTENT_TYPE_MISMATCH";
    /** Antivirus detected malware. */
    public static final String CODE_MALWARE_DETECTED = "UPLOAD_MALWARE_DETECTED";
    /** Antivirus service unavailable (fail-closed policy). */
    public static final String CODE_AV_UNAVAILABLE = "UPLOAD_AV_UNAVAILABLE";
    /** User not authorized to upload in this context. */
    public static final String CODE_FORBIDDEN_UPLOAD = "UPLOAD_FORBIDDEN_UPLOAD";

    public static AppException forbiddenFileType(String message) {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_FORBIDDEN_FILE_TYPE, message);
    }

    public static AppException suspiciousFilename(String message) {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_SUSPICIOUS_FILENAME, message);
    }

    public static AppException extensionMismatch(String message) {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_EXTENSION_MISMATCH, message);
    }

    /** 413 Payload Too Large */
    public static AppException fileTooLarge(long maxBytes) {
        return Errors.of(HttpStatus.PAYLOAD_TOO_LARGE, CODE_FILE_TOO_LARGE,
            "File size exceeds maximum allowed size of " + (maxBytes / (1024 * 1024)) + " MB");
    }

    public static AppException contentTypeMismatch(String message) {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_CONTENT_TYPE_MISMATCH, message);
    }

    /** Safe message for user; signature name must not be exposed. */
    public static AppException malwareDetected() {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_MALWARE_DETECTED, "File rejected");
    }

    /** 503 when antivirus is unavailable and fail-closed policy applies. */
    public static AppException avUnavailable(String message) {
        return Errors.of(HttpStatus.SERVICE_UNAVAILABLE, CODE_AV_UNAVAILABLE, message);
    }

    public static AppException forbiddenUpload(String message) {
        return Errors.of(HttpStatus.FORBIDDEN, CODE_FORBIDDEN_UPLOAD, message);
    }
}
