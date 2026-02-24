/**
 * Adapters that connect Schedule and Offering modules without creating a circular dependency.
 *
 * <h2>Purpose</h2>
 * <ul>
 *   <li>Schedule needs to validate that an offering exists when creating a lesson.</li>
 *   <li>Offering needs to validate that a group exists when creating/updating an offering (groups live in Group module).</li>
 *   <li>Offering needs to validate that a room exists when creating/updating an offering (rooms live in Schedule).</li>
 *   <li>Offering needs timeslot info (day of week) for lesson generation.</li>
 *   <li>Offering needs to create/delete lessons in bulk for lesson generation.</li>
 * </ul>
 * Direct dependencies between modules would create circular dependencies. This package implements the consumer ports
 * of each module by delegating to the other module's ports or APIs.
 *
 * <h2>Adapters</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.adapter.OfferingLookupAdapter} - implements Schedule's
 *       {@link com.example.interhubdev.schedule.OfferingLookupPort} using Offering's
 *       {@link com.example.interhubdev.offering.OfferingLookupDataPort} (minimal, repo-only; avoids cycle).</li>
 *   <li>{@link com.example.interhubdev.adapter.GroupLookupAdapter} - implements Schedule's
 *       {@link com.example.interhubdev.schedule.GroupLookupPort} using {@link com.example.interhubdev.group.GroupExistsPort}
 *       and {@link com.example.interhubdev.group.GroupSummaryPort} (lightweight ports to avoid circular dependencies).</li>
 *   <li>{@link com.example.interhubdev.adapter.GroupLookupAdapterForOffering} - implements Offering's
 *       {@link com.example.interhubdev.offering.GroupLookupPort} using {@link com.example.interhubdev.group.GroupExistsPort}
 *       (lightweight port that only checks existence, avoiding circular dependencies).</li>
 *   <li>{@link com.example.interhubdev.adapter.ScheduleRoomLookupAdapter} - implements Offering's
 *       {@link com.example.interhubdev.offering.RoomLookupPort} using Schedule's
 *       {@link com.example.interhubdev.schedule.RoomExistsPort}.</li>
 *   <li>{@link com.example.interhubdev.adapter.ScheduleTimeslotLookupAdapter} - implements Offering's
 *       {@link com.example.interhubdev.offering.TimeslotLookupPort} using Schedule's
 *       {@link com.example.interhubdev.schedule.ScheduleApi}.</li>
 *   <li>{@link com.example.interhubdev.adapter.ScheduleLessonCreationAdapter} - implements Offering's
 *       {@link com.example.interhubdev.offering.LessonCreationPort} using Schedule's
 *       {@link com.example.interhubdev.schedule.ScheduleApi}.</li>
 *   <li>{@link com.example.interhubdev.adapter.LessonEnrichmentAdapter} - implements Schedule's
 *       {@link com.example.interhubdev.schedule.LessonEnrichmentPort} using Offering's
 *       {@link com.example.interhubdev.offering.LessonEnrichmentDataPort}.</li>
 *   <li>{@link com.example.interhubdev.adapter.LessonLookupAdapter} - implements Document's
 *       {@link com.example.interhubdev.document.LessonLookupPort} using Schedule's
 *       {@link com.example.interhubdev.schedule.ScheduleApi} (for homework lesson validation).</li>
 *   <li>{@link com.example.interhubdev.adapter.OfferingLookupAdapterForDocument} - implements Document's
 *       {@link com.example.interhubdev.document.OfferingLookupPort} using Offering's
 *       {@link com.example.interhubdev.offering.OfferingExistsPort} (for course material offering validation).</li>
 *   <li>{@link com.example.interhubdev.adapter.StoredFileUsageAdapterForDocument} - implements Document's
 *       {@link com.example.interhubdev.document.api.StoredFileUsagePort} using Submission's
 *       {@link com.example.interhubdev.submission.SubmissionApi#isStoredFileInUse} (prevents deleting files attached to submissions).</li>
 *   <li>{@link com.example.interhubdev.adapter.CurriculumSubjectLookupAdapter} - implements Offering's
 *       {@link com.example.interhubdev.offering.CurriculumSubjectLookupPort} using Program's
 *       {@link com.example.interhubdev.program.ProgramApi} (to avoid circular dependency between offering and program).</li>
 *   <li>{@link com.example.interhubdev.adapter.SubjectCurriculumSubjectLookupAdapter} - implements Subject's
 *       {@link com.example.interhubdev.subject.CurriculumSubjectLookupPort} using Program's
 *       {@link com.example.interhubdev.program.ProgramApi} (to avoid circular dependency between subject and program).</li>
 *   <li>{@link com.example.interhubdev.adapter.SemesterIdByYearAdapter} - implements Program's
 *       {@link com.example.interhubdev.program.SemesterIdByYearPort} using Academic's
 *       {@link com.example.interhubdev.academic.AcademicApi} (so Program can resolve semester ID by year and number without depending on Academic).</li>
 *   <li>{@link com.example.interhubdev.adapter.GroupStartYearAdapter} - implements Program's
 *       {@link com.example.interhubdev.program.GroupStartYearPort} using Group's
 *       {@link com.example.interhubdev.group.GroupStartYearPort} (so Program can get group start year without depending on Group).</li>
 *   <li>{@link com.example.interhubdev.adapter.GroupCurriculumIdAdapter} - implements Program's
 *       {@link com.example.interhubdev.program.GroupCurriculumIdPort} using Group's
 *       {@link com.example.interhubdev.group.GroupCurriculumIdPort} (so Program can get curriculum ID from group without depending on Group).</li>
 *   <li>{@link com.example.interhubdev.adapter.GroupIdsByTeacherAdapter} - implements Group's
 *       {@link com.example.interhubdev.group.port.GroupIdsByTeacherPort} using Offering and Schedule (group IDs where teacher has at least one lesson).</li>
 * </ul>
 *
 * <h2>Dependencies</h2>
 * This package depends on {@code schedule}, {@code offering}, {@code group}, {@code document}, {@code subject}, {@code program}, {@code academic}, and {@code submission}
 * (only their port interfaces and types used in method signatures). The modules themselves do not depend on each other.
 * 
 * <p>This package is not a Spring Modulith module - it's an adapter layer that connects modules
 * without creating circular dependencies. It uses only public APIs (ports, DTOs) from the modules.
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Adapter",
    allowedDependencies = {"schedule", "offering", "group", "group :: port", "document", "document :: api", "subject", "program", "academic", "submission", "error"}
)
package com.example.interhubdev.adapter;
