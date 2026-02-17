package com.example.interhubdev.document.internal.uploadSecurity;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Detects MIME type from file content (magic bytes).
 * Used to validate that declared content-type matches actual content, since client-provided
 * Content-Type can be spoofed.
 *
 * <p>Phase 2 roadmap: extend to Office OpenXML (docx/xlsx) validation by peeking inside ZIP
 * for word/document.xml or xl/workbook.xml.
 */
interface ContentSniffer {

    /**
     * Detects MIME type from file content signature (magic bytes).
     *
     * @param file path to file (must exist and be readable)
     * @return detected MIME (e.g. "application/pdf") or empty if unknown/unsupported
     */
    Optional<String> detectMimeFromContent(Path file);
}
