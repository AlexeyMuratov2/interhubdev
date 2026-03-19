package com.example.interhubdev.storedfile;

/**
 * Context for delivery policy: how the file may be delivered. Delivery is allowed only if
 * the file's FileSafetyClass permits this context (e.g. ATTACHMENT_ONLY for general user files).
 */
public enum DeliveryContext {

    /** File served only as attachment/download with safe headers (no inline, no sniffing). */
    ATTACHMENT_ONLY
}
