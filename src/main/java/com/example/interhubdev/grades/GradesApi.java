package com.example.interhubdev.grades;

import com.example.interhubdev.error.AppException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Public API for Grades (Progress) module: ledger of point allocations per student per offering.
 * All errors are thrown as {@link AppException} and handled by global exception handler.
 * Only users with TEACHER or ADMIN/MODERATOR/SUPER_ADMIN can create, update, or void entries.
 */
public interface GradesApi {

    /**
     * Create a single grade entry.
     *
     * @param studentId            student profile id (required)
     * @param offeringId            offering id (required)
     * @param points                points to allocate (required)
     * @param typeCode              allocation type (required); CUSTOM requires typeLabel
     * @param typeLabel             required when typeCode is CUSTOM, must be null otherwise
     * @param description           optional comment
     * @param lessonSessionId       optional link to lesson
     * @param homeworkSubmissionId  optional link to submission (primary when present)
     * @param gradedAt              optional; if null, current time is used
     * @param gradedBy              user id who creates the entry (required)
     * @return created entry DTO
     * @throws AppException NOT_FOUND (offering/student), BAD_REQUEST (validation), FORBIDDEN
     */
    GradeEntryDto create(
            UUID studentId,
            UUID offeringId,
            BigDecimal points,
            GradeTypeCode typeCode,
            String typeLabel,
            String description,
            UUID lessonSessionId,
            UUID homeworkSubmissionId,
            LocalDateTime gradedAt,
            UUID gradedBy
    );

    /**
     * Bulk create grade entries for an offering (same type/description/lesson; per-item studentId and points).
     * All-or-nothing transaction: if any item fails validation, entire batch is rolled back.
     *
     * @param offeringId            offering id (required)
     * @param typeCode              allocation type (required)
     * @param typeLabel             required when typeCode is CUSTOM
     * @param description           optional
     * @param lessonSessionId       optional
     * @param gradedAt              optional; null = current time
     * @param items                 list of {studentId, points, optional homeworkSubmissionId}
     * @param gradedBy              user id (required)
     * @return list of created entry DTOs
     * @throws AppException NOT_FOUND, BAD_REQUEST, FORBIDDEN
     */
    List<GradeEntryDto> createBulk(
            UUID offeringId,
            GradeTypeCode typeCode,
            String typeLabel,
            String description,
            UUID lessonSessionId,
            LocalDateTime gradedAt,
            List<BulkGradeItem> items,
            UUID gradedBy
    );

    /**
     * Update an existing grade entry (points, type, description, links, gradedAt).
     * Entry must be ACTIVE. gradedBy is not changed on update (audit can be extended later).
     *
     * @param id                    entry id
     * @param points                optional new points
     * @param typeCode              optional new type
     * @param typeLabel             optional (required when typeCode is CUSTOM)
     * @param description           optional
     * @param lessonSessionId       optional
     * @param homeworkSubmissionId  optional
     * @param gradedAt              optional
     * @param requesterId           current user (must have permission to manage grades)
     * @return updated entry DTO
     * @throws AppException NOT_FOUND, BAD_REQUEST (e.g. CUSTOM without typeLabel), FORBIDDEN
     */
    GradeEntryDto update(
            UUID id,
            BigDecimal points,
            GradeTypeCode typeCode,
            String typeLabel,
            String description,
            UUID lessonSessionId,
            UUID homeworkSubmissionId,
            LocalDateTime gradedAt,
            UUID requesterId
    );

    /**
     * Soft-delete (void) a grade entry. Entry status becomes VOIDED and is excluded from totals.
     *
     * @param id       entry id
     * @param gradedBy user id performing the void (must have permission to manage grades)
     * @throws AppException NOT_FOUND, FORBIDDEN
     */
    void voidEntry(UUID id, UUID gradedBy);

    /**
     * Get grade entry by id.
     *
     * @param id          entry id
     * @param requesterId current user (must have permission to view grades)
     * @return optional entry DTO
     */
    Optional<GradeEntryDto> getById(UUID id, UUID requesterId);

    /**
     * Get all grade entries for a student in an offering, plus total and breakdown by type.
     *
     * @param studentId     student profile id
     * @param offeringId    offering id
     * @param from          optional filter: gradedAt >= from
     * @param to            optional filter: gradedAt <= to
     * @param includeVoided if true, include VOIDED entries in list (they are never counted in total/breakdown)
     * @param requesterId   current user (must have permission to view grades)
     * @return entries, totalPoints, breakdownByType
     * @throws AppException NOT_FOUND if offering missing, FORBIDDEN if requester cannot view grades
     */
    StudentOfferingGradesDto getStudentOfferingGrades(
            UUID studentId,
            UUID offeringId,
            LocalDateTime from,
            LocalDateTime to,
            boolean includeVoided,
            UUID requesterId
    );

    /**
     * Get summary of grades for all students in a group for one offering.
     * Roster is obtained via Student API (students in group). Only ACTIVE entries count in totals.
     *
     * @param groupId       student group id
     * @param offeringId    offering id (must belong to this group)
     * @param from          optional filter by gradedAt
     * @param to            optional filter by gradedAt
     * @param includeVoided if true, include VOIDED in per-student entry lists (not in totals)
     * @param requesterId   current user (must have permission to view grades)
     * @return list of rows: studentId, totalPoints, breakdownByType
     * @throws AppException NOT_FOUND (group or offering), BAD_REQUEST if offering not for this group, FORBIDDEN
     */
    GroupOfferingSummaryDto getGroupOfferingSummary(
            UUID groupId,
            UUID offeringId,
            LocalDateTime from,
            LocalDateTime to,
            boolean includeVoided,
            UUID requesterId
    );

    /**
     * Get total points per student for a single lesson (ACTIVE entries with lesson_id = lessonSessionId and no homework submission).
     * Homework points are a separate type; this returns only lesson points. Used by composition for the lesson screen roster.
     *
     * @param lessonSessionId lesson (session) id
     * @param requesterId     current user (must have permission to view grades: TEACHER or ADMIN)
     * @return summary with one row per student who has at least one grade entry for this lesson
     * @throws AppException FORBIDDEN if requester cannot view grades
     */
    LessonGradesSummaryDto getLessonGradesSummary(UUID lessonSessionId, UUID requesterId);

    /**
     * Set or replace points for one student for one lesson (UX: single cell edit).
     * Only lesson-only entries (no homework submission) are considered. If the student already has such ACTIVE
     * entries, the first is updated to the new points value and the rest are voided. If there are none,
     * a new entry is created with the given points, linked to this lesson (no homework submission).
     *
     * @param lessonSessionId lesson (session) id
     * @param studentId       student profile id
     * @param points          new points value
     * @param requesterId     user performing the action (must have permission to manage grades)
     * @return the grade entry that now holds the points for this lesson (created or updated)
     * @throws AppException NOT_FOUND (lesson/offering/student), BAD_REQUEST (student not in lesson's group, validation), FORBIDDEN
     */
    GradeEntryDto setPointsForLesson(UUID lessonSessionId, UUID studentId, BigDecimal points, UUID requesterId);

    /**
     * Get points (sum of ACTIVE entries) per homework submission ID.
     * Used by composition to show grades for each submitted homework in one call.
     *
     * @param submissionIds list of homework submission UUIDs (may be empty)
     * @param requesterId   current user (must have permission to view grades)
     * @return map submissionId -> total points; only submissions that have at least one ACTIVE entry are present
     * @throws AppException FORBIDDEN if requester cannot view grades
     */
    Map<UUID, BigDecimal> getPointsByHomeworkSubmissionIds(List<UUID> submissionIds, UUID requesterId);

    /**
     * Get one ACTIVE grade entry per homework submission ID (latest by gradedAt when multiple exist).
     * Used by composition to return full GradeEntryDto per submission for the homework-submissions endpoint.
     *
     * @param submissionIds list of homework submission UUIDs (may be empty)
     * @param requesterId   current user (must have permission to view grades)
     * @return map submissionId -> grade entry; only submissions that have at least one ACTIVE entry are present
     * @throws AppException FORBIDDEN if requester cannot view grades
     */
    Map<UUID, GradeEntryDto> getGradeEntriesByHomeworkSubmissionIds(List<UUID> submissionIds, UUID requesterId);

    /**
     * Get total points (sum of ACTIVE entries) for one student in one offering.
     * Students can view their own total; teachers and admins can view any student's total.
     *
     * @param studentId   student profile id
     * @param offeringId  offering id
     * @param requesterId current user (must be the student's own user, or teacher/admin)
     * @return total points (BigDecimal.ZERO if no entries)
     * @throws AppException NOT_FOUND if offering missing, FORBIDDEN if unauthorized
     */
    BigDecimal getStudentTotalPoints(UUID studentId, UUID offeringId, UUID requesterId);
}
