package com.example.interhubdev.storedfile;

/**
 * Safety class of a file: maximum allowed use. Assigned after acceptance, immutable once ACTIVE.
 * Delivery is the intersection of this class and the current DeliveryContext.
 */
public enum FileSafetyClass {

    /**
     * General-purpose user binary; only controlled attachment/download, no inline, no trust in MIME.
     */
    GENERAL_USER_ATTACHMENT_ONLY
}
