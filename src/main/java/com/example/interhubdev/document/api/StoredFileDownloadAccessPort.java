package com.example.interhubdev.document.api;

import java.util.UUID;

/**
 * Port for allowing download of stored files in specific contexts (e.g. teacher downloading
 * submission files for a homework they teach). Implemented by adapters that know business rules.
 * If any registered port returns true for (storedFileId, userId), download is allowed in addition
 * to the default rules (owner or admin/moderator).
 */
public interface StoredFileDownloadAccessPort {

    /**
     * Check whether the user can download this stored file in a context outside normal ownership
     * (e.g. teacher downloading a student's submission file for a homework they are assigned to).
     *
     * @param storedFileId stored file UUID
     * @param userId       current user UUID
     * @return true if the user is allowed to download this file in the port's context
     */
    boolean canDownload(UUID storedFileId, UUID userId);
}
