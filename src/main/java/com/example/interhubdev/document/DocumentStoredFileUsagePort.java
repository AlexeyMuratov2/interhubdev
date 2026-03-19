package com.example.interhubdev.document;

import java.util.UUID;

/**
 * Port for querying whether a stored file is referenced by any document entity
 * (course material, lesson material file, homework file).
 * Used by adapters (e.g. storedfile's StoredFileUsagePort) to prevent deletion of files in use.
 * Implemented inside document module with repository-only dependencies (no StoredFileApi),
 * so adapters can depend on this port without creating a circular dependency with storedfile.
 */
public interface DocumentStoredFileUsagePort {

    /**
     * Check if the given stored file is referenced by any document attachment.
     *
     * @param storedFileId stored file UUID
     * @return true if any course material, lesson material file, or homework file references this file
     */
    boolean isStoredFileInUse(UUID storedFileId);
}
