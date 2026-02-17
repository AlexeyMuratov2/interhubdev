/**
 * Document module - file storage (upload, download, preview, delete).
 * 
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.document.DocumentApi} - file operations (stored files layer)</li>
 *   <li>{@link com.example.interhubdev.document.CourseMaterialApi} - course materials operations (business layer)</li>
 *   <li>{@link com.example.interhubdev.document.HomeworkApi} - homework assignments linked to lessons</li>
 *   <li>{@link com.example.interhubdev.document.StoredFileDto} - stored file metadata DTO</li>
 *   <li>{@link com.example.interhubdev.document.CourseMaterialDto} - course material DTO (includes StoredFileDto)</li>
 *   <li>{@link com.example.interhubdev.document.HomeworkDto} - homework DTO</li>
 *   <li>{@link com.example.interhubdev.document.LessonLookupPort} - port to check lesson existence (implemented by adapter)</li>
 *   <li>{@link com.example.interhubdev.document.StoragePort} - storage port (S3-compatible)</li>
 *   <li>{@link com.example.interhubdev.document.UploadSecurityPort} - upload security (allowed types, malicious file checks)</li>
 *   <li>{@link com.example.interhubdev.document.UploadContext} - context for upload security check</li>
 *   <li>{@link com.example.interhubdev.document.UploadResult} - upload result from storage port</li>
 * </ul>
 * 
 * <h2>Dependencies</h2>
 * <ul>
 *   <li>error - all business errors via {@link com.example.interhubdev.error.Errors}</li>
 *   <li>auth - current user for upload and access control (AuthApi.getCurrentUser)</li>
 *   <li>user - user roles for permission checks (UserApi)</li>
 * </ul>
 * 
 * <h2>Storage</h2>
 * Uses MinIO (S3-compatible) for file storage. Upload is atomic: on DB failure after S3 upload,
 * the file is removed from S3. Can be replaced with AWS S3 by implementing StoragePort.
 * 
 * <h2>Access control</h2>
 * All endpoints require authentication.
 * <p>
 * Stored files (DocumentApi): File access (download/preview/delete) requires:
 * - File owner (uploadedBy == currentUser), OR
 * - User with ADMIN/MODERATOR/SUPER_ADMIN role.
 * Metadata (GET /stored/{id}) is available to all authenticated users.
 * <p>
 * Course materials (CourseMaterialApi): 
 * - Create/upload: Requires TEACHER or ADMIN/MODERATOR/SUPER_ADMIN role.
 * - List/get: Available to all authenticated users.
 * - Delete: Requires material author or ADMIN/MODERATOR/SUPER_ADMIN role.
 * <p>
 * File deletion safety: Cannot delete a stored file if it is referenced by any course material.
 * <p>
 * Homework: assignments linked to lessons (optional file). Clearing file reference does not delete the file.
 * Lesson existence is validated via {@link com.example.interhubdev.document.LessonLookupPort} (adapter → schedule).
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Document",
    allowedDependencies = {"error", "auth", "user"}
)
package com.example.interhubdev.document;
