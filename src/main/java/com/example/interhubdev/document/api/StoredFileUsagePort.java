package com.example.interhubdev.document.api;

import java.util.UUID;

/**
 * Port for checking if a stored file is in use by other modules (e.g. homework submissions).
 * Implemented by adapter that may delegate to submission or other modules.
 * Used when deleting a stored file to prevent removing files that are still referenced.
 */
public interface StoredFileUsagePort {

    /**
     * Check if the given stored file is referenced by any business entity outside document's own tables.
     *
     * @param storedFileId stored file UUID
     * @return true if the file is in use and should not be deleted
     */
    boolean isStoredFileInUse(UUID storedFileId);
}
