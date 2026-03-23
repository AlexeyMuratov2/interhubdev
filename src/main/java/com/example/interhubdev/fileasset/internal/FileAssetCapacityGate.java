package com.example.interhubdev.fileasset.internal;

import com.example.interhubdev.fileasset.internal.policy.AntivirusMode;
import com.example.interhubdev.fileasset.internal.policy.FileSecurityPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

/**
 * Computes the effective upload ceiling while runtime and scanner capacity are lower than the
 * long-term policy ceiling.
 */
@Component
class FileAssetCapacityGate {

    private final long multipartMaxBytes;
    private final long antivirusMaxScannableBytes;

    @Autowired
    FileAssetCapacityGate(
        @Value("${spring.servlet.multipart.max-file-size:1GB}") DataSize multipartMaxFileSize,
        @Value("${clamav.max-scannable-bytes:524288000}") long antivirusMaxScannableBytes
    ) {
        this.multipartMaxBytes = multipartMaxFileSize.toBytes();
        this.antivirusMaxScannableBytes = antivirusMaxScannableBytes;
    }

    FileAssetCapacityGate(long multipartMaxBytes, long antivirusMaxScannableBytes) {
        this.multipartMaxBytes = multipartMaxBytes;
        this.antivirusMaxScannableBytes = antivirusMaxScannableBytes;
    }

    long effectiveMaxBytes(FileSecurityPolicy policy) {
        long effective = Math.min(policy.maxSizeBytes(), multipartMaxBytes);
        if (policy.antivirusMode() == AntivirusMode.REQUIRED_FAIL_CLOSED) {
            effective = Math.min(effective, antivirusMaxScannableBytes);
        }
        return effective;
    }

    void ensureWithinEffectiveLimit(FileSecurityPolicy policy, long sizeBytes) {
        long effectiveMaxBytes = effectiveMaxBytes(policy);
        if (sizeBytes > effectiveMaxBytes) {
            throw FileAssetErrors.capacityMismatch(sizeBytes, effectiveMaxBytes, policy.maxSizeBytes());
        }
    }
}
