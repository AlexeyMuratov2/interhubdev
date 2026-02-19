/**
 * Offering module - group subject offerings, offering teachers, weekly slots,
 * and automatic lesson generation (Layer 3 delivery).
 *
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.offering.OfferingApi} - offerings, teachers, slots, and lesson generation (facade)</li>
 *   <li>{@link com.example.interhubdev.offering.GroupSubjectOfferingDto}, {@link com.example.interhubdev.offering.OfferingTeacherItemDto},
 *       {@link com.example.interhubdev.offering.OfferingSlotDto} - DTOs (teachers list derived from main teacher and slot teachers)</li>
 *   <li>{@link com.example.interhubdev.offering.GroupLookupPort} - port for group lookup (implemented by adapter)</li>
 *   <li>{@link com.example.interhubdev.offering.TimeslotLookupPort} - port for timeslot info (implemented by adapter)</li>
 *   <li>{@link com.example.interhubdev.offering.LessonCreationPort} - port for lesson creation (implemented by adapter)</li>
 *   <li>{@link com.example.interhubdev.offering.CurriculumSubjectLookupPort} - port for curriculum subject lookup (implemented by adapter)</li>
 * </ul>
 *
 * <h2>Internal structure</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.offering.internal.OfferingServiceImpl} - facade implementing OfferingApi</li>
 *   <li>{@link com.example.interhubdev.offering.internal.OfferingCatalogService} - CRUD for offerings and derived teachers list</li>
 *   <li>{@link com.example.interhubdev.offering.internal.OfferingSlotService} - weekly slot management</li>
 *   <li>{@link com.example.interhubdev.offering.internal.LessonGenerationService} - automatic lesson generation</li>
 *   <li>{@link com.example.interhubdev.offering.internal.OfferingMappers} - entity to DTO mapping</li>
 *   <li>{@link com.example.interhubdev.offering.internal.OfferingValidation} - format/role/lessonType normalization</li>
 * </ul>
 *
 * <h2>Access control</h2>
 * Write operations (create/update/delete/generate) only for roles: MODERATOR, ADMIN, SUPER_ADMIN.
 * Read operations for all authenticated users.
 *
 * <h2>Dependencies</h2>
 * <ul>
 *   <li>program - offerings reference curriculum subject DTOs (via CurriculumSubjectLookupPort to avoid circular dependency)</li>
 *   <li>teacher - offerings and offering teachers reference teachers</li>
 *   <li>academic - lesson generation uses semester dates</li>
 *   <li>error - all business errors via {@link com.example.interhubdev.error.Errors}</li>
 * </ul>
 * Groups, rooms, timeslots, curriculum subjects and lesson creation are accessed via ports (implemented by adapters).
 *
 * <h2>Error codes (via {@link com.example.interhubdev.error.Errors})</h2>
 * <ul>
 *   <li>NOT_FOUND (404) - offering, group, curriculum subject, teacher, room, timeslot, semester, or slot not found</li>
 *   <li>CONFLICT (409) - offering/slot/teacher already exists; lessons already generated (use regenerate)</li>
 *   <li>BAD_REQUEST (400) - invalid format/role/lessonType; offering has no slots for generation</li>
 *   <li>VALIDATION_FAILED (400) - request validation failed (@Valid on create/update)</li>
 *   <li>FORBIDDEN (403) - user has no MODERATOR/ADMIN/SUPER_ADMIN role for write operations</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Offering",
    allowedDependencies = {"program", "teacher", "academic", "error"}
)
package com.example.interhubdev.offering;
