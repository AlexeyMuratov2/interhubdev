/**
 * Submission module.
 *
 * <p>Owns business lifecycle of homework submissions and their attachment
 * bindings. Technical file storage, scanning, processing, and controlled
 * delivery are delegated to the {@code fileasset} module.</p>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Submission",
    allowedDependencies = {"document", "fileasset", "auth", "user", "error", "schedule", "offering", "teacher", "program", "subject", "outbox"}
)
package com.example.interhubdev.submission;
