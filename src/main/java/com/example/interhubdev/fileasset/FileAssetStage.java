package com.example.interhubdev.fileasset;

/**
 * Coarse-grained processing stage exposed to business modules and HTTP clients.
 */
public enum FileAssetStage {
    RECEIVED,
    SCANNING,
    FINALIZING,
    READY,
    FAILED
}
