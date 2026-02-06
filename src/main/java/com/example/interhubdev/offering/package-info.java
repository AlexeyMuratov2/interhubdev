/**
 * Offering module - group subject offerings, offering teachers, weekly slots,
 * and automatic lesson generation (Layer 3 delivery).
 *
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.offering.OfferingApi} - offerings, teachers, slots, and lesson generation (facade)</li>
 *   <li>{@link com.example.interhubdev.offering.GroupSubjectOfferingDto}, {@link com.example.interhubdev.offering.OfferingTeacherDto},
 *       {@link com.example.interhubdev.offering.OfferingSlotDto} - DTOs</li>
 *   <li>{@link com.example.interhubdev.offering.TimeslotLookupPort} - port for timeslot info (implemented by adapter)</li>
 *   <li>{@link com.example.interhubdev.offering.LessonCreationPort} - port for lesson creation (implemented by adapter)</li>
 * </ul>
 *
 * <h2>Internal structure</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.offering.internal.OfferingServiceImpl} - facade implementing OfferingApi</li>
 *   <li>{@link com.example.interhubdev.offering.internal.OfferingCatalogService} - CRUD for group subject offerings</li>
 *   <li>{@link com.example.interhubdev.offering.internal.OfferingTeacherService} - offering teachers (add/remove/list)</li>
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
 *   <li>group - offerings belong to a group</li>
 *   <li>program - offerings reference curriculum subject (durationWeeks for lesson generation)</li>
 *   <li>teacher - offerings and offering teachers reference teachers</li>
 *   <li>schedule - offerings reference rooms and timeslots (via ports); lessons created via port</li>
 *   <li>academic - lesson generation uses semester dates</li>
 *   <li>error - all business errors via {@link com.example.interhubdev.error.Errors}</li>
 * </ul>
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
    allowedDependencies = {"group", "program", "teacher", "schedule", "academic", "error"}
)
package com.example.interhubdev.offering;
