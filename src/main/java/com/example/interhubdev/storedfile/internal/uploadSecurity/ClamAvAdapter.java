package com.example.interhubdev.storedfile.internal.uploadSecurity;

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
 * ClamAV adapter for antivirus scanning.
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
        log.info("ClamAV adapter initialized: {}:{}", host, port);
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
            log.warn("ClamAV size limit exceeded: {}", e.getMessage());
            return ScanResult.error();
        } catch (IOException e) {
            log.warn("ClamAV scan failed: {}", e.getMessage());
            return ScanResult.error();
        } catch (Exception e) {
            log.error("Unexpected ClamAV error: {}", e.getMessage(), e);
            return ScanResult.error();
        }
    }

    private static String extractSignature(String reply) {
        int found = reply.indexOf("FOUND:");
        if (found >= 0) {
            return reply.substring(found + 6).trim();
        }
        return reply.trim();
    }
}
