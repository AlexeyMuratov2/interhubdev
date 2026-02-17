package com.example.interhubdev.document.internal.uploadSecurity;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Detects MIME type from magic bytes (file signature).
 * Supports: PDF, PNG, JPEG, GIF, WebP, ZIP (and Office OpenXML as ZIP).
 *
 * <p>Office OpenXML (docx/xlsx): detected as application/zip. Full validation (peek inside
 * for word/document.xml or xl/workbook.xml) is planned for Phase 2.
 */
@Component
class MagicBytesSniffer implements ContentSniffer {

    private static final int MAX_READ = 16 * 1024; // 16 KB

    private static final byte[] PDF = "%PDF".getBytes();
    private static final byte[] PNG = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] JPEG = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] GIF87a = "GIF87a".getBytes();
    private static final byte[] GIF89a = "GIF89a".getBytes();
    private static final byte[] ZIP = new byte[]{0x50, 0x4B, 0x03, 0x04};
    private static final byte[] ZIP_EMPTY = new byte[]{0x50, 0x4B, 0x05, 0x06};
    private static final byte[] ZIP_SPANNED = new byte[]{0x50, 0x4B, 0x07, 0x08};
    private static final byte[] WEBP_RIFF = "RIFF".getBytes();
    private static final byte[] WEBP_WEBP = "WEBP".getBytes();
    private static final byte[] TXT_UTF8_BOM = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    @Override
    public Optional<String> detectMimeFromContent(Path file) {
        if (file == null || !Files.isRegularFile(file)) {
            return Optional.empty();
        }
        byte[] head;
        try {
            head = Files.readAllBytes(file);
        } catch (IOException e) {
            return Optional.empty();
        }
        if (head.length < 4) {
            return Optional.empty();
        }
        int len = Math.min(head.length, MAX_READ);

        if (startsWith(head, len, PDF)) {
            return Optional.of("application/pdf");
        }
        if (startsWith(head, len, PNG)) {
            return Optional.of("image/png");
        }
        if (startsWith(head, len, JPEG)) {
            return Optional.of("image/jpeg");
        }
        if (startsWith(head, len, GIF87a) || startsWith(head, len, GIF89a)) {
            return Optional.of("image/gif");
        }
        if (startsWith(head, len, ZIP) || startsWith(head, len, ZIP_EMPTY) || startsWith(head, len, ZIP_SPANNED)) {
            return Optional.of("application/zip");
        }
        if (len >= 12 && startsWith(head, 4, WEBP_RIFF) && startsAt(head, 8, WEBP_WEBP)) {
            return Optional.of("image/webp");
        }
        // CSV/txt: no reliable magic; heuristic: plain text. Do not enforce to avoid false positives.
        return Optional.empty();
    }

    private static boolean startsWith(byte[] data, int len, byte[] prefix) {
        if (prefix.length > len) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }

    private static boolean startsAt(byte[] data, int offset, byte[] prefix) {
        if (offset + prefix.length > data.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[offset + i] != prefix[i]) return false;
        }
        return true;
    }
}
