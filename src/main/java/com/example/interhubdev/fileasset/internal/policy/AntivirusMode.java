package com.example.interhubdev.fileasset.internal.policy;

/**
 * Internal antivirus requirement profile for a file policy.
 */
public enum AntivirusMode {

    /**
     * Legacy mode used by pinned older policies that do not require the new AV pipeline.
     */
    DISABLED,

    /**
     * Antivirus is mandatory; any scan failure blocks activation.
     */
    REQUIRED_FAIL_CLOSED
}
