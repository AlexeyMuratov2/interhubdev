package com.example.interhubdev.fileasset.internal.antivirus;

import fi.solita.clamav.ClamAVClient;
import fi.solita.clamav.ClamAVSizeLimitException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

/**
 * ClamAV-backed antivirus adapter owned by the fileasset module.
 */
@Component
@Slf4j
class ClamAvFileAssetAdapter implements FileAssetAntivirusPort {

    private final ClamAVClient client;
    private final boolean enabled;
    private final AntivirusCapabilities capabilities;

    ClamAvFileAssetAdapter(
        @Value("${clamav.host:localhost}") String host,
        @Value("${clamav.port:3310}") int port,
        @Value("${clamav.timeout-ms:30000}") int timeoutMs,
        @Value("${clamav.enabled:false}") boolean enabled,
        @Value("${clamav.max-scannable-bytes:524288000}") long maxScannableBytes
    ) {
        this.client = new ClamAVClient(host, port, timeoutMs);
        this.enabled = enabled;
        this.capabilities = new AntivirusCapabilities(maxScannableBytes);
    }

    @Override
    public AntivirusCapabilities capabilities() {
        return capabilities;
    }

    @Override
    public ScanVerdict scan(InputStream inputStream, long sizeBytes, String originalName, String declaredContentType) {
        if (!enabled) {
            return ScanVerdict.error(ScanFailureReason.UNAVAILABLE);
        }
        if (inputStream == null) {
            return ScanVerdict.error(ScanFailureReason.SCAN_ERROR);
        }
        try (InputStream is = inputStream) {
            byte[] reply = client.scan(is);
            if (ClamAVClient.isCleanReply(reply)) {
                return ScanVerdict.clean();
            }
            String replyStr = new String(reply, StandardCharsets.US_ASCII);
            return ScanVerdict.infected(extractSignature(replyStr));
        } catch (ClamAVSizeLimitException e) {
            log.warn("Fileasset scan failed because ClamAV size limit was exceeded: {}", e.getMessage());
            return ScanVerdict.error(ScanFailureReason.SIZE_LIMIT_EXCEEDED);
        } catch (IOException e) {
            log.warn("Fileasset scan I/O failure: {}", e.getMessage());
            return ScanVerdict.error(classifyFailure(e));
        } catch (Exception e) {
            log.warn("Fileasset scan unexpected failure: {}", e.getMessage());
            return ScanVerdict.error(classifyFailure(e));
        }
    }

    private static ScanFailureReason classifyFailure(Exception exception) {
        Throwable cursor = exception;
        while (cursor != null) {
            if (cursor instanceof SocketTimeoutException) {
                return ScanFailureReason.TIMEOUT;
            }
            if (cursor instanceof ConnectException) {
                return ScanFailureReason.UNAVAILABLE;
            }
            cursor = cursor.getCause();
        }
        String message = exception.getMessage();
        if (message != null) {
            String lower = message.toLowerCase();
            if (lower.contains("timeout")) {
                return ScanFailureReason.TIMEOUT;
            }
            if (lower.contains("connection refused") || lower.contains("unavailable")) {
                return ScanFailureReason.UNAVAILABLE;
            }
        }
        return ScanFailureReason.SCAN_ERROR;
    }

    private static String extractSignature(String reply) {
        int found = reply.indexOf("FOUND:");
        if (found >= 0) {
            return reply.substring(found + 6).trim();
        }
        return reply.trim();
    }
}
