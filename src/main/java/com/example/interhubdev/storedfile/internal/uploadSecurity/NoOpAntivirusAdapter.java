package com.example.interhubdev.storedfile.internal.uploadSecurity;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

/**
 * No-op antivirus when ClamAV is disabled.
 */
@Component
@ConditionalOnProperty(name = "clamav.enabled", havingValue = "false", matchIfMissing = true)
class NoOpAntivirusAdapter implements AntivirusPort {

    @Override
    public ScanResult scan(Path file, String filename, String contentType) {
        return ScanResult.clean();
    }
}
