package com.example.interhubdev.storedfile.internal.uploadSecurity;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Detects MIME type from file content (magic bytes).
 */
interface ContentSniffer {

    Optional<String> detectMimeFromContent(Path file);
}
