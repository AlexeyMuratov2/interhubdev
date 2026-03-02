package com.example.interhubdev.composition;

import java.util.UUID;

/**
 * Read-only query API for student grade history in an offering.
 */
public interface StudentGradeHistoryQueryApi {

    /**
     * Get full grade history for a student in an offering.
     * All grade entries with lesson, homework, submission and grader context.
     *
     * @param studentId   student profile ID (must not be null)
     * @param offeringId  offering ID (must not be null)
     * @param requesterId current authenticated user ID (student may view own; teacher/admin may view any)
     * @return aggregated DTO with totalPoints, breakdownByType, entries with lesson/homework/submission/gradedBy
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if offering not found, FORBIDDEN if requester cannot view grades
     */
    StudentGradeHistoryDto getStudentGradeHistory(UUID studentId, UUID offeringId, UUID requesterId);
}
