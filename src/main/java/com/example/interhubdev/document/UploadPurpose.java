package com.example.interhubdev.document;

/**
 * Upload purpose for scenario-based security policy.
 * Used by {@link UploadContext} to apply different rules (e.g. who may upload, max size).
 */
public enum UploadPurpose {
    /** Generic upload (baseline policy: any authenticated user). */
    GENERIC,
    /** Course material attachment (e.g. TEACHER/ADMIN only). */
    COURSE_MATERIAL,
    /** Homework submission (e.g. STUDENT for own submission). */
    HOMEWORK_SUBMISSION
}
