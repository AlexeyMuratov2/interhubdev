package com.example.interhubdev.composition;

import java.util.UUID;

/**
 * Read-only query API for student homework history in an offering.
 */
public interface StudentHomeworkHistoryQueryApi {

    /**
     * Get full homework history for a student in an offering.
     * All homework assignments for the offering and the student's submission and grade per assignment.
     *
     * @param studentId   student profile ID (must not be null)
     * @param offeringId  offering ID (must not be null)
     * @param requesterId current authenticated user ID (student may view own; teacher/admin may view any)
     * @return aggregated DTO with student, subjectName, items (homework + lesson + submission + grade + files per row)
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if offering or student not found, FORBIDDEN if requester cannot view
     */
    StudentHomeworkHistoryDto getStudentHomeworkHistory(UUID studentId, UUID offeringId, UUID requesterId);
}
