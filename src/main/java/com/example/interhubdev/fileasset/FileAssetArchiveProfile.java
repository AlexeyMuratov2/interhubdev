package com.example.interhubdev.fileasset;

/**
 * Archive handling profile decided by fileasset policy execution.
 */
public enum FileAssetArchiveProfile {

    /**
     * Default archive handling without special unpacking or archive-specific checks.
     */
    STANDARD,

    /**
     * Archives and containers are stored as opaque blobs and must never be unpacked server-side.
     */
    OPAQUE_NO_SERVER_EXTRACTION
}
