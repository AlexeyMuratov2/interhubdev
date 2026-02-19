/**
 * Attendance module - official attendance records marked by teachers for lesson sessions.
 *
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.attendance.AttendanceApi} - mark attendance (bulk/single), get by session/student/group</li>
 *   <li>{@link com.example.interhubdev.attendance.AttendanceRecordDto}, {@link com.example.interhubdev.attendance.SessionAttendanceDto},
 *       {@link com.example.interhubdev.attendance.StudentAttendanceDto}, {@link com.example.interhubdev.attendance.GroupAttendanceSummaryDto} - DTOs</li>
 *   <li>{@link com.example.interhubdev.attendance.AttendanceStatus} - enum: PRESENT, ABSENT, LATE, EXCUSED</li>
 * </ul>
 *
 * <h2>Model</h2>
 * Each record in attendance_record represents one official attendance mark by a teacher for a student in a lesson session.
 * Status can be PRESENT, ABSENT, LATE (with optional minutesLate), or EXCUSED (with optional teacherComment).
 * One record per student per lesson session (unique constraint).
 *
 * <h2>Access control</h2>
 * <ul>
 *   <li>Mark attendance: TEACHER (only for own sessions) or ADMIN/MODERATOR/SUPER_ADMIN</li>
 *   <li>Read by session: TEACHER (only for own sessions) or ADMIN/MODERATOR/SUPER_ADMIN</li>
 *   <li>Read by student: STUDENT (only own records) or TEACHER/ADMIN/MODERATOR/SUPER_ADMIN</li>
 *   <li>Read by group: TEACHER (for groups they teach) or ADMIN/MODERATOR/SUPER_ADMIN</li>
 * </ul>
 *
 * <h2>Dependencies</h2>
 * <ul>
 *   <li>schedule - get lesson by id, validate lesson exists</li>
 *   <li>offering - get offering by id to get groupId and teacherIds for authorization</li>
 *   <li>student - get roster by group, validate student exists</li>
 *   <li>group - validate group exists</li>
 *   <li>teacher - get teacher profile by userId for authorization checks</li>
 *   <li>auth - current user for marked_by and authorization checks</li>
 *   <li>user - roles for permission checks</li>
 *   <li>error - AppException, Errors</li>
 *   <li>document - file storage (for absence notice attachments; file IDs stored, validation optional)</li>
 * </ul>
 *
 * <h2>Error codes (via {@link com.example.interhubdev.attendance.internal.AttendanceErrors} or {@link com.example.interhubdev.error.Errors})</h2>
 * <ul>
 *   <li>ATTENDANCE_LESSON_NOT_FOUND, ATTENDANCE_STUDENT_NOT_FOUND, ATTENDANCE_GROUP_NOT_FOUND, ATTENDANCE_OFFERING_NOT_FOUND (404)</li>
 *   <li>ATTENDANCE_FORBIDDEN - user cannot mark/read attendance for this session (403)</li>
 *   <li>ATTENDANCE_STUDENT_NOT_IN_GROUP - student not in session's group roster (400)</li>
 *   <li>ATTENDANCE_VALIDATION_FAILED - invalid status/minutesLate combination, etc. (400)</li>
 * </ul>
 *
 * <h2>Subsystems</h2>
 * <ul>
 *   <li><b>attendance.records</b> - Official attendance records marked by teachers (attendance_record table)</li>
 *   <li><b>attendance.notice</b> - Student absence notices (absence_notice table with attachments)</li>
 * </ul>
 *
 * <h2>Future extensions</h2>
 * <ul>
 *   <li>Integration events: AttendanceMarked event for notifications/push (TODO markers in code)</li>
 *   <li>Integration events: AbsenceNoticeSubmittedOrUpdated event for notifications (TODO in notice use-cases)</li>
 *   <li>Link absence notices to attendance records (attached_record_id field prepared, not implemented)</li>
 *   <li>Attendance percentage calculation: computed aggregates per student/group</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Attendance",
    allowedDependencies = {"schedule", "offering", "student", "group", "teacher", "auth", "user", "error", "document"}
)
package com.example.interhubdev.attendance;
