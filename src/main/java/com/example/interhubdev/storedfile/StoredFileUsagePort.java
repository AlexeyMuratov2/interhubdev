package com.example.interhubdev.storedfile;

import java.util.UUID;

/**
 * Port for checking if a stored file is in use by business entities (e.g. document attachments, submissions).
 * Implemented by adapter that queries document and submission modules.
 * Used when deleting a stored file to prevent removing files that are still referenced.
 */
public interface StoredFileUsagePort {

    /**
     * Check if the given stored file is referenced by any business entity.
     *
     * @param storedFileId stored file UUID
     * @return true if the file is in use and should not be deleted
     */
    boolean isStoredFileInUse(UUID storedFileId);
}
