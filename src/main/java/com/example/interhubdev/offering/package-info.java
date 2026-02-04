/**
 * Offering module - group subject offerings and offering teachers (Layer 3 delivery).
 *
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.offering.OfferingApi} - offerings and offering teachers (facade)</li>
 *   <li>{@link com.example.interhubdev.offering.GroupSubjectOfferingDto}, {@link com.example.interhubdev.offering.OfferingTeacherDto} - DTOs</li>
 * </ul>
 *
 * <h2>Internal structure</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.offering.internal.OfferingServiceImpl} - facade implementing OfferingApi</li>
 *   <li>{@link com.example.interhubdev.offering.internal.OfferingCatalogService} - CRUD for group subject offerings</li>
 *   <li>{@link com.example.interhubdev.offering.internal.OfferingTeacherService} - offering teachers (add/remove/list)</li>
 *   <li>{@link com.example.interhubdev.offering.internal.OfferingMappers} - entity to DTO mapping</li>
 *   <li>{@link com.example.interhubdev.offering.internal.OfferingValidation} - format/role normalization and validation</li>
 * </ul>
 *
 * <h2>Access control</h2>
 * Write operations (create/update/delete) only for roles: MODERATOR, ADMIN, SUPER_ADMIN. Read operations for all authenticated users.
 *
 * <h2>Dependencies</h2>
 * <ul>
 *   <li>group - offerings belong to a group</li>
 *   <li>program - offerings reference curriculum subject</li>
 *   <li>teacher - offerings and offering teachers reference teachers</li>
 *   <li>schedule - offerings may reference room</li>
 *   <li>error - all business errors via {@link com.example.interhubdev.error.Errors}</li>
 * </ul>
 *
 * <h2>Error codes (via {@link com.example.interhubdev.error.Errors})</h2>
 * <ul>
 *   <li>NOT_FOUND (404) - offering, group, curriculum subject, teacher, room or offering teacher not found</li>
 *   <li>CONFLICT (409) - offering already exists for group and curriculum subject; offering teacher with this role already exists</li>
 *   <li>BAD_REQUEST (400) - group/curriculum subject id required; format must be offline/online/mixed; teacher id/role required; role must be LECTURE/PRACTICE/LAB</li>
 *   <li>VALIDATION_FAILED (400) - request validation failed (@Valid on create/update)</li>
 *   <li>FORBIDDEN (403) - user has no MODERATOR/ADMIN/SUPER_ADMIN role for write operations</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Offering",
    allowedDependencies = {"group", "program", "teacher", "schedule", "error"}
)
package com.example.interhubdev.offering;
