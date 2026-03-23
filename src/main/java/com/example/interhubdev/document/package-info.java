/**
 * Document module.
 *
 * <p>Owns business entities that can contain attachments: homework,
 * lesson materials, and course materials. Technical file lifecycle, storage,
 * scanning, and controlled delivery are delegated to the {@code fileasset}
 * module.</p>
 *
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.document.CourseMaterialApi}</li>
 *   <li>{@link com.example.interhubdev.document.HomeworkApi}</li>
 *   <li>{@link com.example.interhubdev.document.LessonMaterialApi}</li>
 *   <li>{@link com.example.interhubdev.document.DocumentAttachmentApi}</li>
 *   <li>{@link com.example.interhubdev.document.CourseMaterialDto}</li>
 *   <li>{@link com.example.interhubdev.document.HomeworkDto}</li>
 *   <li>{@link com.example.interhubdev.document.LessonMaterialDto}</li>
 *   <li>{@link com.example.interhubdev.document.DocumentAttachmentDto}</li>
 *   <li>{@link com.example.interhubdev.document.LessonLookupPort}</li>
 *   <li>{@link com.example.interhubdev.document.OfferingLookupPort}</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Document",
    allowedDependencies = {"error", "auth", "user", "fileasset"}
)
package com.example.interhubdev.document;
