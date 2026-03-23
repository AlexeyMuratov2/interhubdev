package com.example.interhubdev.web;

import com.example.interhubdev.fileasset.FileAssetUploadCommand;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Prepared upload commands backed by temporary files that must be cleaned up by the caller.
 */
public record MultipartUploadBundle(List<FileAssetUploadCommand> uploads, List<Path> tempFiles) implements AutoCloseable {

    @Override
    public void close() {
        for (Path tempFile : tempFiles) {
            try {
                Files.deleteIfExists(tempFile);
            } catch (Exception ignored) {
                // Best-effort temp cleanup.
            }
        }
    }
}
