package com.example.interhubdev.composition;

import java.util.UUID;

/**
 * Read-only query API for student attendance history in an offering.
 */
public interface StudentAttendanceHistoryQueryApi {

    /**
     * Get attendance history for a student in an offering.
     * All lessons for the offering with attendance record and absence notices per lesson; summary counts.
     *
     * @param studentId   student profile ID (must not be null)
     * @param offeringId  offering ID (must not be null)
     * @param requesterId current authenticated user ID (student can only view own; teacher/admin can view any)
     * @return aggregated DTO with student, subjectName, missedCount, absenceNoticesSubmittedCount, lessons list
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if offering or student not found, FORBIDDEN if requester cannot view
     */
    StudentAttendanceHistoryDto getStudentAttendanceHistory(UUID studentId, UUID offeringId, UUID requesterId);
}
