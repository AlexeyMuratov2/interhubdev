/**
 * Attendance module (facade) – unified API for attendance records and absence notices.
 *
 * <p>Delegates to {@code attendancerecord} and {@code absencenotice} modules.
 * Clients (composition, notification) depend only on this module.</p>
 *
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.attendance.AttendanceApi} – mark and query attendance, session view, notices, create/update/cancel/respond</li>
 *   <li>Merged DTOs: {@link com.example.interhubdev.attendance.SessionAttendanceDto}, {@link com.example.interhubdev.attendance.StudentAttendanceByLessonsDto}, etc.</li>
 * </ul>
 *
 * <h2>Dependencies</h2>
 * attendancerecord, absencenotice, auth, error.
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Attendance",
    allowedDependencies = {"attendancerecord", "absencenotice", "auth", "error", "student", "outbox"}
)
package com.example.interhubdev.attendance;
