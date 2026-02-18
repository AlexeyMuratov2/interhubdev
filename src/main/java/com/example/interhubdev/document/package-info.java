/**
 * Document module - file storage (upload, download, preview, delete).
 * 
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.document.DocumentApi} - file operations (stored files layer)</li>
 *   <li>{@link com.example.interhubdev.document.CourseMaterialApi} - course materials operations (business layer)</li>
 *   <li>{@link com.example.interhubdev.document.HomeworkApi} - homework assignments linked to lessons</li>
 *   <li>{@link com.example.interhubdev.document.LessonMaterialApi} - lesson materials (one lesson many materials, one material many files)</li>
 *   <li>{@link com.example.interhubdev.document.StoredFileDto} - stored file metadata DTO</li>
 *   <li>{@link com.example.interhubdev.document.CourseMaterialDto} - course material DTO (includes StoredFileDto)</li>
 *   <li>{@link com.example.interhubdev.document.HomeworkDto} - homework DTO</li>
 *   <li>{@link com.example.interhubdev.document.LessonMaterialDto} - lesson material DTO (includes list of StoredFileDto)</li>
 *   <li>{@link com.example.interhubdev.document.LessonLookupPort} - port to check lesson existence (implemented by adapter)</li>
 *   <li>{@link com.example.interhubdev.document.OfferingLookupPort} - port to check offering existence (implemented by adapter)</li>
 *   <li>{@link com.example.interhubdev.document.api.StoredFileUsagePort} - port to check if stored file is in use by other modules (e.g. submissions)</li>
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
 * File deletion safety: Cannot delete a stored file if it is referenced by any course material, homework, or lesson material.
 * <p>
 * Lesson materials: materials for a specific lesson (tables {@code lesson_material}, {@code lesson_material_file}).
 * One lesson has many materials; one material has many files. Lesson existence validated via {@link com.example.interhubdev.document.LessonLookupPort}.
 * Create/list/get/delete and add/remove files via {@link com.example.interhubdev.document.LessonMaterialApi}.
 * <p>
 * Course materials: linked to {@code group_subject_offering} (specific delivery of subject to group with teacher).
 * This allows each teacher to have their own materials for the same subject in different groups.
 * Offering existence is validated via {@link com.example.interhubdev.document.OfferingLookupPort} (adapter → offering).
 * Database-level FK ensures referential integrity between course materials and offerings.
 * <p>
 * Homework: assignments linked to lessons via junction table {@code lesson_homework} with FK constraints.
 * Optional file reference: clearing file reference does not delete the file.
 * Lesson existence is validated via {@link com.example.interhubdev.document.LessonLookupPort} (adapter → schedule).
 * Database-level FK ensures referential integrity between homework and lessons.
 * <p>
 * Lesson materials: same access rules as course materials (TEACHER or ADMIN for create; author or ADMIN for delete/modify).
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Document",
    allowedDependencies = {"error", "auth", "user"}
)
package com.example.interhubdev.document;
