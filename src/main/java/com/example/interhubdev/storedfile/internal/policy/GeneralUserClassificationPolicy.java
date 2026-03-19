package com.example.interhubdev.storedfile.internal.policy;

import com.example.interhubdev.storedfile.FileSafetyClass;
import com.example.interhubdev.storedfile.UploadContextKey;
import org.springframework.stereotype.Component;

/**
 * Classification for GENERAL_USER_FILE: GENERAL_USER_ATTACHMENT_ONLY.
 */
@Component
class GeneralUserClassificationPolicy implements ClassificationPolicy {

    @Override
    public FileSafetyClass classify(UploadContextKey contextKey) {
        if (contextKey == UploadContextKey.GENERAL_USER_FILE) {
            return FileSafetyClass.GENERAL_USER_ATTACHMENT_ONLY;
        }
        throw new IllegalArgumentException("Unknown upload context key: " + contextKey);
    }
}
