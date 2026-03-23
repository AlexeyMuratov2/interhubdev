package com.example.interhubdev.fileasset.internal;

import com.example.interhubdev.error.AppException;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.fileasset.FileAssetStatus;
import com.example.interhubdev.fileasset.FilePolicyKey;
import org.springframework.http.HttpStatus;

import java.util.UUID;

/**
 * Error codes and factories for the fileasset module.
 */
public final class FileAssetErrors {

    public static final String CODE_FILE_ASSET_NOT_FOUND = "FILE_ASSET_NOT_FOUND";
    public static final String CODE_UNSUPPORTED_POLICY = "FILE_ASSET_UNSUPPORTED_POLICY";
    public static final String CODE_POLICY_DEFINITION_MISSING = "FILE_ASSET_POLICY_DEFINITION_MISSING";
    public static final String CODE_INVALID_TRANSITION = "FILE_ASSET_INVALID_TRANSITION";
    public static final String CODE_UPLOAD_RECEIPT_CONFLICT = "FILE_ASSET_UPLOAD_RECEIPT_CONFLICT";
    public static final String CODE_INVALID_UPLOAD_RECEIPT = "FILE_ASSET_INVALID_UPLOAD_RECEIPT";
    public static final String CODE_INVALID_REGISTRATION = "FILE_ASSET_INVALID_REGISTRATION";
    public static final String CODE_CAPACITY_MISMATCH = "FILE_ASSET_CAPACITY_MISMATCH";
    public static final String CODE_UPLOADED_OBJECT_MISSING = "FILE_ASSET_UPLOADED_OBJECT_MISSING";
    public static final String CODE_MALWARE_DETECTED = "FILE_ASSET_MALWARE_DETECTED";
    public static final String CODE_ANTIVIRUS_UNAVAILABLE = "FILE_ASSET_ANTIVIRUS_UNAVAILABLE";
    public static final String CODE_ANTIVIRUS_TIMEOUT = "FILE_ASSET_ANTIVIRUS_TIMEOUT";
    public static final String CODE_SCANNER_CAPACITY_EXCEEDED = "FILE_ASSET_SCANNER_CAPACITY_EXCEEDED";
    public static final String CODE_SCAN_FAILED = "FILE_ASSET_SCAN_FAILED";

    private FileAssetErrors() {
    }

    public static AppException fileAssetNotFound(UUID id) {
        return Errors.of(HttpStatus.NOT_FOUND, CODE_FILE_ASSET_NOT_FOUND, "File asset not found: " + id);
    }

    public static AppException unsupportedPolicy(FilePolicyKey key) {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_UNSUPPORTED_POLICY, "Unsupported file policy: " + key);
    }

    public static AppException policyDefinitionMissing(FilePolicyKey key, int version) {
        return Errors.of(HttpStatus.INTERNAL_SERVER_ERROR, CODE_POLICY_DEFINITION_MISSING,
            "Policy definition is missing for " + key + " version " + version);
    }

    public static AppException invalidTransition(UUID id, FileAssetStatus status, String attemptedAction) {
        return Errors.of(HttpStatus.CONFLICT, CODE_INVALID_TRANSITION,
            "Cannot " + attemptedAction + " file asset " + id + " from state " + status);
    }

    public static AppException uploadReceiptConflict(UUID id) {
        return Errors.of(HttpStatus.CONFLICT, CODE_UPLOAD_RECEIPT_CONFLICT,
            "Upload receipt does not match the existing uploaded content for file asset " + id);
    }

    public static AppException invalidUploadReceipt() {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_INVALID_UPLOAD_RECEIPT,
            "Upload receipt must contain uploadToken, checksum or etag.");
    }

    public static AppException invalidRegistration(String message) {
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_INVALID_REGISTRATION, message);
    }

    public static AppException capacityMismatch(long requestedBytes, long effectiveMaxBytes, long policyMaxBytes) {
        return Errors.of(HttpStatus.PAYLOAD_TOO_LARGE, CODE_CAPACITY_MISMATCH,
            "File size " + requestedBytes + " exceeds current runtime capacity " + effectiveMaxBytes
                + " bytes (policy ceiling: " + policyMaxBytes + " bytes).");
    }

    public static AppException uploadedObjectMissing(UUID id) {
        return Errors.of(HttpStatus.CONFLICT, CODE_UPLOADED_OBJECT_MISSING,
            "Uploaded content is missing from temporary storage for file asset " + id);
    }

    public static AppException malwareDetected(String signatureName) {
        String message = "File rejected by antivirus.";
        if (signatureName != null && !signatureName.isBlank()) {
            message += " Signature: " + signatureName;
        }
        return Errors.of(HttpStatus.BAD_REQUEST, CODE_MALWARE_DETECTED, message);
    }

    public static AppException antivirusUnavailable() {
        return Errors.of(HttpStatus.SERVICE_UNAVAILABLE, CODE_ANTIVIRUS_UNAVAILABLE,
            "Antivirus service is unavailable; the file cannot be activated.");
    }

    public static AppException antivirusTimeout() {
        return Errors.of(HttpStatus.SERVICE_UNAVAILABLE, CODE_ANTIVIRUS_TIMEOUT,
            "Antivirus scan timed out; the file cannot be activated.");
    }

    public static AppException scannerCapacityExceeded(long requestedBytes, long maxScannableBytes) {
        return Errors.of(HttpStatus.PAYLOAD_TOO_LARGE, CODE_SCANNER_CAPACITY_EXCEEDED,
            "Antivirus can scan up to " + maxScannableBytes + " bytes, but received " + requestedBytes + " bytes.");
    }

    public static AppException scanFailed() {
        return Errors.of(HttpStatus.SERVICE_UNAVAILABLE, CODE_SCAN_FAILED,
            "File scan failed; the file cannot be activated.");
    }
}
