package com.example.interhubdev.fileasset.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Internal cleanup job for expiring orphaned or unfinished file assets.
 */
@Component
@RequiredArgsConstructor
class FileAssetCleanupJob {

    private final FileAssetServiceImpl fileAssetService;

    @Scheduled(fixedDelayString = "${fileasset.cleanup.interval:60000}")
    void cleanupExpiredAssets() {
        fileAssetService.expireStaleAssets(LocalDateTime.now());
    }
}
