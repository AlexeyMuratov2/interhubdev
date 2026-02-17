package com.example.interhubdev.document.internal.uploadSecurity;

import fi.solita.clamav.ClamAVClient;
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
        } catch (IOException e) {
            log.warn("ClamAV scan failed for file (size={}): {}", file.toAbsolutePath(), e.getMessage());
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
