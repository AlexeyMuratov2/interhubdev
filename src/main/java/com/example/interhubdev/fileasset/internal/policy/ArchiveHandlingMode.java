package com.example.interhubdev.fileasset.internal.policy;

/**
 * Internal archive/container handling semantics for a file policy.
 */
public enum ArchiveHandlingMode {

    /**
     * Legacy mode kept for pinned older policies.
     */
    STANDARD,

    /**
     * Archive-like files are treated as opaque blobs and must not be unpacked server-side.
     */
    OPAQUE_NO_SERVER_EXTRACTION
}
