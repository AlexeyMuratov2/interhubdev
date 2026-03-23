package com.example.interhubdev.fileasset;

/**
 * Delivery constraints derived from fileasset policy execution.
 */
public enum FileDeliveryProfile {

    /**
     * Legacy controlled attachment profile kept for pinned v1 assets.
     */
    CONTROLLED_ATTACHMENT_ONLY,

    /**
     * File may only be delivered through backend-controlled attachment streaming.
     * Presigned delivery and inline preview are forbidden.
     */
    BACKEND_ATTACHMENT_STREAM_ONLY
}
