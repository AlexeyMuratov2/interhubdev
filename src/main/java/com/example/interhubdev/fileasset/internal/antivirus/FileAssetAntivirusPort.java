package com.example.interhubdev.fileasset.internal.antivirus;

import java.io.InputStream;

/**
 * Internal antivirus contract owned by the fileasset module.
 */
public interface FileAssetAntivirusPort {

    AntivirusCapabilities capabilities();

    ScanVerdict scan(InputStream inputStream, long sizeBytes, String originalName, String declaredContentType);

    record ScanVerdict(Status status, String signatureName, ScanFailureReason failureReason) {

        public enum Status {
            CLEAN,
            INFECTED,
            ERROR
        }

        public static ScanVerdict clean() {
            return new ScanVerdict(Status.CLEAN, null, null);
        }

        public static ScanVerdict infected(String signatureName) {
            return new ScanVerdict(Status.INFECTED, signatureName, null);
        }

        public static ScanVerdict error(ScanFailureReason reason) {
            return new ScanVerdict(Status.ERROR, null, reason);
        }
    }
}
