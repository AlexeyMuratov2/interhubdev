/**
 * Attendance Record module – official attendance records marked by teachers for lesson sessions.
 *
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.attendancerecord.AttendanceRecordApi} – mark attendance (bulk/single), get by session/student/group, attach/detach notice</li>
 *   <li>DTOs: AttendanceRecordDto, SessionRecordsDto, MarkAttendanceItem, AttendanceStatus, GroupAttendanceSummaryDto, StudentAttendanceDto, etc.</li>
 * </ul>
 *
 * <p>This module does not contain absence notice logic; it only stores and exposes optional
 * {@code absenceNoticeId} on records. Validation of notice attachment is done by the facade or absence-notice module.</p>
 *
 * <h2>Dependencies</h2>
 * schedule, offering, student, group, teacher, auth, user, error.
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Attendance Record",
    allowedDependencies = {"schedule", "offering", "student", "group", "teacher", "auth", "user", "error"}
)
package com.example.interhubdev.attendancerecord;
