package com.example.interhubdev.document.internal.uploadSecurity;

import fi.solita.clamav.ClamAVClient;
import fi.solita.clamav.ClamAVSizeLimitException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

/**
 * ClamAV adapter: scans files via clamd (INSTREAM). Implements {@link AntivirusPort}.
 * Enabled when {@code clamav.enabled=true}.
 */
@Component
@ConditionalOnProperty(name = "clamav.enabled", havingValue = "true")
@Slf4j
class ClamAvAdapter implements AntivirusPort {

    private final ClamAVClient client;

    ClamAvAdapter(
            @Value("${clamav.host:localhost}") String host,
            @Value("${clamav.port:3310}") int port,
            @Value("${clamav.timeout-ms:30000}") int timeoutMs
    ) {
        this.client = new ClamAVClient(host, port, timeoutMs);
        log.info("ClamAV adapter initialized: connecting to {}:{} (timeout: {}ms)", host, port, timeoutMs);
        log.info("Environment check - CLAMAV_HOST={}, CLAMAV_PORT={}, CLAMAV_ENABLED={}", 
            System.getenv("CLAMAV_HOST"), System.getenv("CLAMAV_PORT"), System.getenv("CLAMAV_ENABLED"));
        log.info("Note: If running in Docker, use service name 'clamav' instead of 'localhost' for CLAMAV_HOST");
    }

    @Override
    public ScanResult scan(Path file, String filename, String contentType) {
        if (file == null || !Files.isRegularFile(file)) {
            return ScanResult.error();
        }
        try (InputStream is = Files.newInputStream(file)) {
            byte[] reply = client.scan(is);
            if (ClamAVClient.isCleanReply(reply)) {
                return ScanResult.clean();
            }
            String replyStr = new String(reply, StandardCharsets.US_ASCII);
            String signature = extractSignature(replyStr);
            return ScanResult.infected(signature);
        } catch (ClamAVSizeLimitException e) {
            try {
                long fileSize = Files.size(file);
                log.warn("ClamAV size limit exceeded for file (size={} bytes, ~{} MB): {}", 
                    fileSize, fileSize / (1024 * 1024), e.getMessage());
            } catch (IOException ioException) {
                log.warn("ClamAV size limit exceeded for file (unable to get file size): {}", e.getMessage());
            }
            log.warn("File exceeds ClamAV StreamMaxLength limit. Consider increasing StreamMaxLength in clamd.conf");
            return ScanResult.error();
        } catch (IOException e) {
            log.warn("ClamAV scan failed for file (size={}): {} - {}", 
                file.toAbsolutePath(), e.getClass().getSimpleName(), e.getMessage());
            log.debug("ClamAV connection error details", e);
            return ScanResult.error();
        } catch (Exception e) {
            log.error("Unexpected error during ClamAV scan: {}", e.getMessage(), e);
            return ScanResult.error();
        }
    }

    private static String extractSignature(String reply) {
        // FOUND: signature.name
        int found = reply.indexOf("FOUND:");
        if (found >= 0) {
            return reply.substring(found + 6).trim();
        }
        return reply.trim();
    }
}
