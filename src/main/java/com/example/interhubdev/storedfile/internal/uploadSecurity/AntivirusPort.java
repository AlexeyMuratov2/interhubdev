package com.example.interhubdev.storedfile.internal.uploadSecurity;

import java.nio.file.Path;

/**
 * Port for antivirus scanning.
 */
interface AntivirusPort {

    ScanResult scan(Path file, String filename, String contentType);

    record ScanResult(Status status, String signatureName) {
        enum Status { CLEAN, INFECTED, ERROR }

        static ScanResult clean() {
            return new ScanResult(Status.CLEAN, null);
        }

        static ScanResult infected(String signatureName) {
            return new ScanResult(Status.INFECTED, signatureName);
        }

        static ScanResult error() {
            return new ScanResult(Status.ERROR, null);
        }
    }
}
