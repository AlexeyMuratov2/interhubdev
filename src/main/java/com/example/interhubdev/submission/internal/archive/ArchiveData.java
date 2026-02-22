package com.example.interhubdev.submission.internal.archive;

import java.util.List;

/**
 * Result of loading archive data: metadata and list of file entries.
 * Internal to submission module (public for cross-package use within module).
 */
public record ArchiveData(ArchiveInfo info, List<ArchiveEntry> entries) {
}
