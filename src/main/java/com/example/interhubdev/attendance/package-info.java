/**
 * Attendance module (orchestrator) – coordinates write operations for attendance records and absence notices.
 *
 * <p>Delegates to {@code attendancerecord} and {@code absencenotice}. Read/query use composition or domain APIs.</p>
 *
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.attendance.AttendanceApi} – mark attendance (bulk/single), create/update/cancel notices, attach/detach notice to record</li>
 * </ul>
 *
 * <h2>Dependencies</h2>
 * attendancerecord, absencenotice, auth, error, student (resolve current student for notices), outbox (events).
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Attendance",
    allowedDependencies = {"attendancerecord", "absencenotice", "auth", "error", "student", "outbox"}
)
package com.example.interhubdev.attendance;
