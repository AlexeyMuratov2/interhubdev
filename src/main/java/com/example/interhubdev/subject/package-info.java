/**
 * Subject module - catalog of subjects and assessment types.
 *
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.subject.SubjectApi} - subjects and assessment types (facade)</li>
 *   <li>{@link com.example.interhubdev.subject.SubjectDto} - subject DTO</li>
 *   <li>{@link com.example.interhubdev.subject.AssessmentTypeDto} - assessment type DTO</li>
 *   <li>{@link com.example.interhubdev.subject.TeacherSubjectListItemDto} - teacher subject list item DTO</li>
 *   <li>{@link com.example.interhubdev.subject.TeacherSubjectDetailDto} - teacher subject detail DTO</li>
 *   <li>{@link com.example.interhubdev.subject.OfferingLookupPort} - port for offering lookup</li>
 *   <li>{@link com.example.interhubdev.subject.SlotLessonsCheckPort} - port to check that offerings have at least one lesson by slot</li>
 *   <li>{@link com.example.interhubdev.subject.TeacherLookupPort} - port for teacher lookup</li>
 *   <li>{@link com.example.interhubdev.subject.CurriculumSubjectLookupPort} - port for curriculum subject lookup</li>
 *   <li>{@link com.example.interhubdev.subject.GroupSubjectOfferingDto} - group subject offering DTO (from offering module)</li>
 * </ul>
 *
 * <h2>Internal structure</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.subject.internal.SubjectServiceImpl} - facade implementing SubjectApi</li>
 *   <li>{@link com.example.interhubdev.subject.internal.SubjectCatalogService} - CRUD for subjects (validates department)</li>
 *   <li>{@link com.example.interhubdev.subject.internal.AssessmentTypeCatalogService} - CRUD for assessment types</li>
 *   <li>{@link com.example.interhubdev.subject.internal.SubjectMappers} - entity to DTO mapping</li>
 *   <li>{@link com.example.interhubdev.subject.internal.SubjectValidation} - code/chineseName/englishName trimming and required-field validation</li>
 * </ul>
 *
 * <h2>Access control</h2>
 * Write operations (create/update/delete) only for roles: MODERATOR, ADMIN, SUPER_ADMIN. Read operations for all authenticated users.
 *
 * <h2>Dependencies</h2>
 * <ul>
 *   <li>department - subjects may reference a department; department existence validated on create/update when departmentId is set</li>
 *   <li>program - curriculum subjects for teacher subjects endpoints (accessed via CurriculumSubjectLookupPort to avoid circular dependency)</li>
 *   <li>offering - group subject offerings for teacher subjects endpoints</li>
 *   <li>document - course materials for teacher subjects endpoints</li>
 *   <li>teacher - teacher lookup for authentication</li>
 *   <li>group - group information for teacher subjects endpoints</li>
 *   <li>user - user information for author names</li>
 *   <li>error - all business errors via {@link com.example.interhubdev.error.Errors}</li>
 * </ul>
 *
 * <h2>Error codes (via {@link com.example.interhubdev.error.Errors})</h2>
 * <ul>
 *   <li>NOT_FOUND (404) - subject, assessment type or department (when departmentId set) not found</li>
 *   <li>CONFLICT (409) - subject or assessment type with given code already exists</li>
 *   <li>BAD_REQUEST (400) - subject/assessment type code is blank or invalid input</li>
 *   <li>VALIDATION_FAILED (400) - request validation failed (@Valid on create/update)</li>
 *   <li>FORBIDDEN (403) - user has no MODERATOR/ADMIN/SUPER_ADMIN role for write operations</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Subject",
    allowedDependencies = {"department", "program", "offering", "document", "teacher", "group", "user", "error"}
)
package com.example.interhubdev.subject;
