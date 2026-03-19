/**
 * Storedfile module – centralised file security and storage (upload, acceptance, classification, delivery, delete).
 * No business domain: does not know about course, lesson, homework, submission.
 * Does not perform business-level authorization (roles, ownership); caller is responsible.
 *
 * <h2>Security model</h2>
 * <ul>
 *   <li><b>Activation gate:</b> Only files with status {@link com.example.interhubdev.storedfile.FileStatus#ACTIVE} can be bound or downloaded.</li>
 *   <li><b>Policies:</b> Acceptance (upload), Classification ({@link com.example.interhubdev.storedfile.FileSafetyClass}), Delivery ({@link com.example.interhubdev.storedfile.DeliveryContext}).</li>
 *   <li><b>Controlled delivery:</b> Content/presigned URL require {@link com.example.interhubdev.storedfile.DeliveryContext}; general flow is ATTACHMENT_ONLY.</li>
 *   <li><b>Deletion:</b> Only via this module's API; deletion by raw storage key is forbidden. DELETED is terminal.</li>
 * </ul>
 *
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.storedfile.StoredFileApi} – upload (with {@link com.example.interhubdev.storedfile.UploadContextKey}), get metadata, get content (with {@link com.example.interhubdev.storedfile.DeliveryContext}), presigned URL, delete</li>
 *   <li>{@link com.example.interhubdev.storedfile.StoredFileMeta} – stored file metadata DTO (includes status, safetyClass)</li>
 *   <li>{@link com.example.interhubdev.storedfile.StoredFileUsagePort} – port to check if file is in use (implemented by adapter)</li>
 *   <li>{@link com.example.interhubdev.storedfile.FileStatus}, {@link com.example.interhubdev.storedfile.FileSafetyClass}, {@link com.example.interhubdev.storedfile.UploadContextKey}, {@link com.example.interhubdev.storedfile.DeliveryContext}</li>
 * </ul>
 *
 * <h2>Dependencies</h2>
 * <ul>
 *   <li>error – AppException, Errors</li>
 * </ul>
 *
 * <h2>Delete</h2>
 * Before deleting, module calls {@link StoredFileUsagePort#isStoredFileInUse}; if true, delete throws CONFLICT. Soft delete: status set to DELETED, object removed from storage.
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Storedfile",
    allowedDependencies = {"error"}
)
package com.example.interhubdev.storedfile;
