package com.example.interhubdev.document.internal.uploadSecurity;

import com.example.interhubdev.document.UploadContext;
import com.example.interhubdev.document.UploadSecurityPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

/**
 * Facade for upload security: enforces policy (size, filename, allowed types, magic bytes, antivirus).
 * Implements {@link UploadSecurityPort}; called at the start of the upload flow in {@link DocumentServiceImpl}.
 *
 * <p>Order of checks: size → filename → allowed types → magic bytes (if contentPath provided) → antivirus.
 */
@Service
@RequiredArgsConstructor
@Slf4j
class UploadSecurityService implements UploadSecurityPort {

    private final MaliciousFileChecks maliciousFileChecks;
    private final AllowedFileTypesPolicy allowedFileTypesPolicy;
    private final ContentSniffer contentSniffer;
    private final AntivirusPort antivirusPort;

    @Value("${app.document.max-file-size-bytes:52428800}")
    private long maxFileSizeBytes;

    @Override
    public void ensureUploadAllowed(UploadContext context, Path contentPath) {
        // 1. File size
        if (context.size() <= 0) {
            throw UploadSecurityErrors.emptyFile("File size must be positive");
        }
        if (context.size() > maxFileSizeBytes) {
            logSecurityEvent(context, "FILE_TOO_LARGE", null);
            throw UploadSecurityErrors.fileTooLarge(maxFileSizeBytes);
        }

        // 2. Filename (malicious patterns)
        maliciousFileChecks.checkFilename(context.originalFilename());

        // 3. Allowed MIME + extension match
        allowedFileTypesPolicy.checkAllowed(context.contentType(), context.originalFilename());

        // 4. Magic bytes (content sniffing) — when content available
        if (contentPath != null) {
            contentSniffer.detectMimeFromContent(contentPath).ifPresent(detectedMime -> {
                String declaredMime = normalizeMime(context.contentType());
                if (!matchesDeclared(detectedMime, declaredMime)) {
                    logSecurityEvent(context, "CONTENT_TYPE_MISMATCH", "detected=" + detectedMime + ", declared=" + declaredMime);
                    throw UploadSecurityErrors.contentTypeMismatch("File content does not match declared type");
                }
            });
        }

        // 5. Antivirus scan — when content available
        if (contentPath != null) {
            AntivirusPort.ScanResult result;
            try {
                result = antivirusPort.scan(contentPath, context.originalFilename(), context.contentType());
            } catch (Exception e) {
                logSecurityEvent(context, "AV_UNAVAILABLE", null);
                throw UploadSecurityErrors.avUnavailable("Antivirus service is temporarily unavailable. Please try again later.");
            }
            switch (result.status()) {
                case INFECTED -> {
                    logSecurityEvent(context, "MALWARE_DETECTED", result.signatureName());
                    throw UploadSecurityErrors.malwareDetected();
                }
                case ERROR -> {
                    logSecurityEvent(context, "AV_UNAVAILABLE", null);
                    throw UploadSecurityErrors.avUnavailable("Antivirus service is temporarily unavailable. Please try again later.");
                }
                default -> { /* CLEAN */ }
            }
        }
    }

    private static String normalizeMime(String contentType) {
        if (contentType == null || contentType.isBlank()) return "";
        return contentType.split(";")[0].trim().toLowerCase();
    }

    /** Detected MIME matches declared (or declared is Office OpenXML and detected is ZIP). */
    private boolean matchesDeclared(String detectedMime, String declaredMime) {
        if (detectedMime.equalsIgnoreCase(declaredMime)) return true;
        if ("application/zip".equalsIgnoreCase(detectedMime) && allowedFileTypesPolicy.isZipBasedMime(declaredMime)) {
            return true;
        }
        return false;
    }

    private void logSecurityEvent(UploadContext ctx, String reasonCode, String detail) {
        String sanitizedFilename = ctx.originalFilename() != null ? ctx.originalFilename().replaceAll("[\\x00-\\x1F\\x7F]", "?") : "?";
        log.warn("Upload security event: userId={}, filename={}, size={}, contentType={}, reason={}, detail={}",
            ctx.uploadedBy(), sanitizedFilename, ctx.size(), ctx.contentType(), reasonCode, detail != null ? detail : "-");
    }
}
