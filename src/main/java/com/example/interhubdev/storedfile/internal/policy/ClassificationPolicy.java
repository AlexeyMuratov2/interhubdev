package com.example.interhubdev.storedfile.internal.policy;

import com.example.interhubdev.storedfile.FileSafetyClass;
import com.example.interhubdev.storedfile.UploadContextKey;

/**
 * Assigns FileSafetyClass for an accepted file by upload context key. Classification is immutable after ACTIVE.
 */
public interface ClassificationPolicy {

    /**
     * Resolve safety class for the given upload context key.
     *
     * @param contextKey upload context key
     * @return safety class to assign after acceptance
     */
    FileSafetyClass classify(UploadContextKey contextKey);
}
