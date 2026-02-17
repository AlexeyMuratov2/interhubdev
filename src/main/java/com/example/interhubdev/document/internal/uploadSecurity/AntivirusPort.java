package com.example.interhubdev.document.internal.uploadSecurity;

import java.nio.file.Path;

/**
 * Port for antivirus scanning. Implemented by ClamAV adapter.
 * Used by {@link UploadSecurityService} during upload flow.
 */
interface AntivirusPort {

    /**
     * Scans file for malware.
     *
     * @param file        path to file to scan
     * @param filename    original filename (for logging)
     * @param contentType declared MIME type
     * @return scan result (CLEAN, INFECTED, ERROR)
     */
    ScanResult scan(Path file, String filename, String contentType);

    /**
     * Result of antivirus scan.
     */
    record ScanResult(Status status, String signatureName) {
        public enum Status { CLEAN, INFECTED, ERROR }

        public static ScanResult clean() {
            return new ScanResult(Status.CLEAN, null);
        }

        public static ScanResult infected(String signatureName) {
            return new ScanResult(Status.INFECTED, signatureName);
        }

        public static ScanResult error() {
            return new ScanResult(Status.ERROR, null);
        }
    }
}
