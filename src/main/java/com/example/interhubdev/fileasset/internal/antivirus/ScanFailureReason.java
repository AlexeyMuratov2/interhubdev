package com.example.interhubdev.fileasset.internal.antivirus;

/**
 * Canonical reasons why an AV scan could not produce a clean verdict.
 */
public enum ScanFailureReason {
    UNAVAILABLE,
    TIMEOUT,
    SIZE_LIMIT_EXCEEDED,
    SCAN_ERROR
}
