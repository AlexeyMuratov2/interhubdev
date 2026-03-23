package com.example.interhubdev.fileasset;

import java.io.InputStream;

/**
 * Controlled backend download handle for an ACTIVE file asset.
 */
public record FileAssetDownloadHandle(
    String originalName,
    long sizeBytes,
    InputStream stream
) {
}
