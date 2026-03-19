package com.example.interhubdev.storedfile;

/**
 * Declarative key for selecting upload policy (acceptance, classification). Adding a new scenario
 * (e.g. avatar) adds a new key and policy implementations without changing the pipeline.
 */
public enum UploadContextKey {

    /** General user file flow: broad acceptance, GENERAL_USER_ATTACHMENT_ONLY class. */
    GENERAL_USER_FILE
}
