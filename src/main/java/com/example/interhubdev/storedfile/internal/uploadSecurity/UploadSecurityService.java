package com.example.interhubdev.storedfile.internal.uploadSecurity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

/**
 * Enforces upload security: size, filename, MIME, magic bytes, antivirus.
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
        if (context.size() <= 0) {
            throw UploadSecurityErrors.emptyFile("File size must be positive");
        }
        if (context.size() > maxFileSizeBytes) {
            logSecurityEvent(context, "FILE_TOO_LARGE", null);
            throw UploadSecurityErrors.fileTooLarge(maxFileSizeBytes);
        }
        maliciousFileChecks.checkFilename(context.originalFilename());
        allowedFileTypesPolicy.checkAllowed(context.contentType(), context.originalFilename());

        if (contentPath != null) {
            contentSniffer.detectMimeFromContent(contentPath).ifPresent(detectedMime -> {
                String declaredMime = normalizeMime(context.contentType());
                if (!matchesDeclared(detectedMime, declaredMime)) {
                    logSecurityEvent(context, "CONTENT_TYPE_MISMATCH", "detected=" + detectedMime + ", declared=" + declaredMime);
                    throw UploadSecurityErrors.contentTypeMismatch("File content does not match declared type");
                }
            });

            AntivirusPort.ScanResult result;
            try {
                result = antivirusPort.scan(contentPath, context.originalFilename(), context.contentType());
            } catch (Exception e) {
                logSecurityEvent(context, "AV_UNAVAILABLE", null);
                throw UploadSecurityErrors.avUnavailable("Antivirus service is temporarily unavailable.");
            }
            if (result.status() == AntivirusPort.ScanResult.Status.INFECTED) {
                logSecurityEvent(context, "MALWARE_DETECTED", result.signatureName());
                throw UploadSecurityErrors.malwareDetected();
            }
            if (result.status() == AntivirusPort.ScanResult.Status.ERROR) {
                logSecurityEvent(context, "AV_UNAVAILABLE", null);
                throw UploadSecurityErrors.avUnavailable("Antivirus service is temporarily unavailable.");
            }
        }
    }

    private static String normalizeMime(String contentType) {
        if (contentType == null || contentType.isBlank()) return "";
        return contentType.split(";")[0].trim().toLowerCase();
    }

    private boolean matchesDeclared(String detectedMime, String declaredMime) {
        if (detectedMime.equalsIgnoreCase(declaredMime)) return true;
        if ("application/zip".equalsIgnoreCase(detectedMime) && allowedFileTypesPolicy.isZipBasedMime(declaredMime)) {
            return true;
        }
        return false;
    }

    private void logSecurityEvent(UploadContext ctx, String reasonCode, String detail) {
        String sanitized = ctx.originalFilename() != null ? ctx.originalFilename().replaceAll("[\\x00-\\x1F\\x7F]", "?") : "?";
        log.warn("Upload security: userId={}, filename={}, size={}, contentType={}, reason={}, detail={}",
            ctx.uploadedBy(), sanitized, ctx.size(), ctx.contentType(), reasonCode, detail != null ? detail : "-");
    }
}
