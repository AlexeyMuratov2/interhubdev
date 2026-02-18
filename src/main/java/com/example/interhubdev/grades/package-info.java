/**
 * Grades (Progress) module — ledger of point allocations per student per offering.
 *
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.grades.GradesApi} — create, update, void entries; get by student/offering; group summary</li>
 *   <li>{@link com.example.interhubdev.grades.GradeEntryDto}, {@link com.example.interhubdev.grades.StudentOfferingGradesDto},
 *       {@link com.example.interhubdev.grades.GroupOfferingSummaryDto} — DTOs</li>
 *   <li>{@link com.example.interhubdev.grades.GradeTypeCode} — enum of allocation types (SEMINAR, EXAM, HOMEWORK, CUSTOM, etc.)</li>
 * </ul>
 *
 * <h2>Model</h2>
 * Each record in the ledger ({@code grade_entry}) is one allocation or correction. Points can be linked optionally
 * to a lesson or homework submission. Totals are computed by summing ACTIVE entries; VOIDED entries are excluded.
 * CUSTOM type requires a teacher-defined type_label.
 *
 * <h2>Access control</h2>
 * Only users with TEACHER or ADMIN/MODERATOR/SUPER_ADMIN can create, update, or void grades. Read by same roles
 * (and optionally students for own grades — extend later).
 *
 * <h2>Dependencies</h2>
 * <ul>
 *   <li>student — roster by group for group summary ({@link com.example.interhubdev.student.StudentApi#findByGroupId})</li>
 *   <li>group — validate group exists for group summary</li>
 *   <li>offering — validate offering exists and belongs to group</li>
 *   <li>auth — current user for graded_by</li>
 *   <li>user — roles for permission checks</li>
 *   <li>error — AppException, Errors</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Grades",
    allowedDependencies = {"student", "group", "offering", "auth", "user", "error"}
)
package com.example.interhubdev.grades;
