package com.example.interhubdev.fileasset.internal.policy;

/**
 * Internal policy dimension describing how strongly the system isolates uploaded files from
 * execution-capable paths.
 */
public enum ExecutionIsolationProfile {

    /**
     * Baseline storage isolation kept for pinned legacy policies.
     */
    BASIC_STORAGE_ISOLATION,

    /**
     * Uploaded files must never be executed, unpacked or rendered server-side.
     */
    NEVER_EXECUTE_SERVER_SIDE
}
