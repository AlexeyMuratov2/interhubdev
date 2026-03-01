/**
 * Absence Notice module – student absence/lateness notices and teacher response workflow.
 *
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.absencenotice.AbsenceNoticeApi} – create, update, cancel, respond, get notices, attach/detach to record</li>
 *   <li>DTOs and enums for notices, pages, summaries.</li>
 * </ul>
 *
 * <p>Depends on attendancerecord for linking notices to attendance records (via port).</p>
 *
 * <h2>Dependencies</h2>
 * attendancerecord, schedule, offering, student, group, teacher, program, auth, user, error, document, outbox.
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Absence Notice",
    allowedDependencies = {"attendancerecord", "schedule", "offering", "student", "group", "teacher", "program", "auth", "user", "error", "document", "outbox"}
)
package com.example.interhubdev.absencenotice;
