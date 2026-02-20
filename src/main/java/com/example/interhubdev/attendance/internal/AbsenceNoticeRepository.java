package com.example.interhubdev.attendance.internal;

import com.example.interhubdev.attendance.AbsenceNoticeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for absence_notice.
 */
interface AbsenceNoticeRepository extends JpaRepository<AbsenceNotice, UUID> {

    /**
     * Find active (SUBMITTED) notice for a student and session.
     */
    @Query("SELECT an FROM AbsenceNotice an WHERE an.lessonSessionId = :sessionId " +
            "AND an.studentId = :studentId AND an.status = :status")
    Optional<AbsenceNotice> findActiveBySessionAndStudent(
            @Param("sessionId") UUID sessionId,
            @Param("studentId") UUID studentId,
            @Param("status") AbsenceNoticeStatus status
    );

    /**
     * Find all notices for a lesson session.
     *
     * @param sessionId lesson session ID
     * @return list of notices (ordered by submittedAt DESC)
     */
    List<AbsenceNotice> findByLessonSessionIdOrderBySubmittedAtDesc(UUID sessionId);

    /**
     * Find all notices for a lesson session with optional status filter.
     *
     * @param sessionId lesson session ID
     * @param status    optional status filter (if null, returns all)
     * @return list of notices (ordered by submittedAt DESC)
     */
    @Query("SELECT an FROM AbsenceNotice an WHERE an.lessonSessionId = :sessionId " +
            "AND (:status IS NULL OR an.status = :status) " +
            "ORDER BY an.submittedAt DESC")
    List<AbsenceNotice> findByLessonSessionIdAndStatus(
            @Param("sessionId") UUID sessionId,
            @Param("status") AbsenceNoticeStatus status
    );

    /**
     * Find all notices for a student within a date range.
     *
     * @param studentId student profile ID
     * @param from      optional filter: submittedAt >= from
     * @param to        optional filter: submittedAt <= to
     * @return list of notices (ordered by submittedAt DESC)
     */
    @Query("SELECT an FROM AbsenceNotice an WHERE an.studentId = :studentId " +
            "AND (:from IS NULL OR an.submittedAt >= :from) " +
            "AND (:to IS NULL OR an.submittedAt <= :to) " +
            "ORDER BY an.submittedAt DESC")
    List<AbsenceNotice> findByStudentIdAndSubmittedAtBetween(
            @Param("studentId") UUID studentId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    /**
     * Find notice by ID and student ID (for ownership check).
     *
     * @param id        notice ID
     * @param studentId student profile ID
     * @return optional notice
     */
    Optional<AbsenceNotice> findByIdAndStudentId(UUID id, UUID studentId);

    /**
     * Find the last submitted notice for a student and session.
     * Used for auto-attach functionality.
     *
     * @param sessionId lesson session ID
     * @param studentId student profile ID
     * @return optional notice (most recent SUBMITTED notice, ordered by submittedAt DESC, then updatedAt DESC)
     */
    @Query("SELECT an FROM AbsenceNotice an WHERE an.lessonSessionId = :sessionId " +
            "AND an.studentId = :studentId AND an.status = :status " +
            "ORDER BY an.submittedAt DESC, an.updatedAt DESC")
    Optional<AbsenceNotice> findLastSubmittedBySessionAndStudent(
            @Param("sessionId") UUID sessionId,
            @Param("studentId") UUID studentId,
            @Param("status") AbsenceNoticeStatus status
    );

    /**
     * Find all notices for a lesson session (for UI display).
     * Returns all notices regardless of status (can be filtered by status in application layer).
     *
     * @param sessionId lesson session ID
     * @return list of notices (ordered by submittedAt DESC)
     */
    List<AbsenceNotice> findByLessonSessionId(UUID sessionId);

    /**
     * First page: find absence notices for lessons (no cursor).
     * Used when cursor is null to avoid binding null parameters (PostgreSQL type inference issue).
     * Note: limit is applied in application layer.
     */
    @Query("SELECT an FROM AbsenceNotice an " +
            "WHERE an.lessonSessionId IN :sessionIds " +
            "ORDER BY an.submittedAt DESC, an.id DESC")
    List<AbsenceNotice> findFirstPageBySessionIds(@Param("sessionIds") List<UUID> sessionIds);

    /**
     * Next page: find absence notices with cursor (cursorSubmittedAt and cursorId are non-null).
     * Note: limit is applied in application layer.
     */
    @Query("SELECT an FROM AbsenceNotice an " +
            "WHERE an.lessonSessionId IN :sessionIds " +
            "AND ((an.submittedAt < :cursorSubmittedAt) OR (an.submittedAt = :cursorSubmittedAt AND an.id < :cursorId)) " +
            "ORDER BY an.submittedAt DESC, an.id DESC")
    List<AbsenceNotice> findNextPageBySessionIds(
            @Param("sessionIds") List<UUID> sessionIds,
            @Param("cursorSubmittedAt") java.time.LocalDateTime cursorSubmittedAt,
            @Param("cursorId") UUID cursorId
    );

    /**
     * First page: find absence notices with status filter (no cursor).
     */
    @Query("SELECT an FROM AbsenceNotice an " +
            "WHERE an.lessonSessionId IN :sessionIds AND an.status IN :statuses " +
            "ORDER BY an.submittedAt DESC, an.id DESC")
    List<AbsenceNotice> findFirstPageBySessionIdsAndStatuses(
            @Param("sessionIds") List<UUID> sessionIds,
            @Param("statuses") List<AbsenceNoticeStatus> statuses
    );

    /**
     * Next page: find absence notices with status filter and cursor.
     */
    @Query("SELECT an FROM AbsenceNotice an " +
            "WHERE an.lessonSessionId IN :sessionIds AND an.status IN :statuses " +
            "AND ((an.submittedAt < :cursorSubmittedAt) OR (an.submittedAt = :cursorSubmittedAt AND an.id < :cursorId)) " +
            "ORDER BY an.submittedAt DESC, an.id DESC")
    List<AbsenceNotice> findNextPageBySessionIdsAndStatuses(
            @Param("sessionIds") List<UUID> sessionIds,
            @Param("statuses") List<AbsenceNoticeStatus> statuses,
            @Param("cursorSubmittedAt") java.time.LocalDateTime cursorSubmittedAt,
            @Param("cursorId") UUID cursorId
    );
}
