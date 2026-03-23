package com.example.interhubdev.fileasset;

import java.util.UUID;

/**
 * Implemented by business modules to tell fileasset whether a file asset is still bound anywhere.
 */
public interface FileAssetUsagePort {

    boolean isFileAssetInUse(UUID fileAssetId);
}
