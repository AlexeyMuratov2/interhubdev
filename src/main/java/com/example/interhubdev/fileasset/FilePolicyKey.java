package com.example.interhubdev.fileasset;

/**
 * Public selector for file security and processing profiles.
 * <p>
 * Business modules choose one of these keys, but the meaning of the policy stays inside the
 * fileasset module.
 */
public enum FilePolicyKey {

    /**
     * Default controlled attachment profile for the initial migration step.
     */
    CONTROLLED_ATTACHMENT
}
