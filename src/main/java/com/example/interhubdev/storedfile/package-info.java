/**
 * Storedfile module – technical file storage (upload, metadata, content stream, delete).
 * No business domain: does not know about course, lesson, homework, submission.
 * Does not perform business-level authorization (roles, ownership); caller is responsible.
 *
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.storedfile.StoredFileApi} – upload, get metadata, get content, presigned URL, delete</li>
 *   <li>{@link com.example.interhubdev.storedfile.StoredFileMeta} – stored file metadata DTO</li>
 *   <li>{@link com.example.interhubdev.storedfile.StoredFileUsagePort} – port to check if file is in use (implemented by adapter)</li>
 * </ul>
 *
 * <h2>Dependencies</h2>
 * <ul>
 *   <li>error – AppException, Errors</li>
 * </ul>
 *
 * <h2>Delete</h2>
 * Before deleting, module calls {@link StoredFileUsagePort#isStoredFileInUse}; if true, delete throws CONFLICT.
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Storedfile",
    allowedDependencies = {"error"}
)
package com.example.interhubdev.storedfile;
