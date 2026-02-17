package com.example.interhubdev.document.internal.uploadSecurity;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * No-op antivirus adapter when ClamAV is disabled ({@code clamav.enabled=false}).
 * Always returns CLEAN; used so {@link UploadSecurityService} does not need optional injection.
 */
@Component
@ConditionalOnProperty(name = "clamav.enabled", havingValue = "false", matchIfMissing = true)
class NoOpAntivirusAdapter implements AntivirusPort {

    @Override
    public ScanResult scan(Path file, String filename, String contentType) {
        return ScanResult.clean();
    }
}
