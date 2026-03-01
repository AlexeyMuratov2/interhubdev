package com.example.interhubdev.absencenotice.internal;

import com.example.interhubdev.absencenotice.AbsenceNoticeStatus;
import org.springframework.data.domain.Pageable;
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

    @Query("SELECT an FROM AbsenceNotice an WHERE an.lessonSessionId = :sessionId " +
            "AND an.studentId = :studentId AND an.status = :status")
    Optional<AbsenceNotice> findActiveBySessionAndStudent(
            @Param("sessionId") UUID sessionId,
            @Param("studentId") UUID studentId,
            @Param("status") AbsenceNoticeStatus status
    );

    List<AbsenceNotice> findByLessonSessionIdOrderBySubmittedAtDesc(UUID sessionId);

    @Query("SELECT an FROM AbsenceNotice an WHERE an.lessonSessionId = :sessionId " +
            "AND (:status IS NULL OR an.status = :status) " +
            "ORDER BY an.submittedAt DESC")
    List<AbsenceNotice> findByLessonSessionIdAndStatus(
            @Param("sessionId") UUID sessionId,
            @Param("status") AbsenceNoticeStatus status
    );

    @Query("SELECT an FROM AbsenceNotice an WHERE an.studentId = :studentId " +
            "AND (:from IS NULL OR an.submittedAt >= :from) " +
            "AND (:to IS NULL OR an.submittedAt <= :to) " +
            "ORDER BY an.submittedAt DESC")
    List<AbsenceNotice> findByStudentIdAndSubmittedAtBetween(
            @Param("studentId") UUID studentId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    Optional<AbsenceNotice> findByIdAndStudentId(UUID id, UUID studentId);

    Optional<AbsenceNotice> findByAttachedRecordId(UUID attachedRecordId);

    @Query("SELECT an FROM AbsenceNotice an WHERE an.lessonSessionId = :sessionId " +
            "AND an.studentId = :studentId AND an.status = :status " +
            "ORDER BY an.submittedAt DESC, an.updatedAt DESC")
    Optional<AbsenceNotice> findLastSubmittedBySessionAndStudent(
            @Param("sessionId") UUID sessionId,
            @Param("studentId") UUID studentId,
            @Param("status") AbsenceNoticeStatus status
    );

    List<AbsenceNotice> findByLessonSessionId(UUID sessionId);

    @Query("SELECT an FROM AbsenceNotice an WHERE an.studentId = :studentId " +
            "AND an.lessonSessionId IN :sessionIds ORDER BY an.lessonSessionId, an.submittedAt DESC")
    List<AbsenceNotice> findByStudentIdAndLessonSessionIdIn(
            @Param("studentId") UUID studentId,
            @Param("sessionIds") List<UUID> sessionIds);

    @Query("SELECT an FROM AbsenceNotice an " +
            "WHERE an.lessonSessionId IN :sessionIds " +
            "ORDER BY an.submittedAt DESC, an.id DESC")
    List<AbsenceNotice> findFirstPageBySessionIds(@Param("sessionIds") List<UUID> sessionIds);

    @Query("SELECT an FROM AbsenceNotice an " +
            "WHERE an.lessonSessionId IN :sessionIds " +
            "AND ((an.submittedAt < :cursorSubmittedAt) OR (an.submittedAt = :cursorSubmittedAt AND an.id < :cursorId)) " +
            "ORDER BY an.submittedAt DESC, an.id DESC")
    List<AbsenceNotice> findNextPageBySessionIds(
            @Param("sessionIds") List<UUID> sessionIds,
            @Param("cursorSubmittedAt") LocalDateTime cursorSubmittedAt,
            @Param("cursorId") UUID cursorId
    );

    @Query("SELECT an FROM AbsenceNotice an " +
            "WHERE an.lessonSessionId IN :sessionIds AND an.status IN :statuses " +
            "ORDER BY an.submittedAt DESC, an.id DESC")
    List<AbsenceNotice> findFirstPageBySessionIdsAndStatuses(
            @Param("sessionIds") List<UUID> sessionIds,
            @Param("statuses") List<AbsenceNoticeStatus> statuses
    );

    @Query("SELECT an FROM AbsenceNotice an " +
            "WHERE an.lessonSessionId IN :sessionIds AND an.status IN :statuses " +
            "AND ((an.submittedAt < :cursorSubmittedAt) OR (an.submittedAt = :cursorSubmittedAt AND an.id < :cursorId)) " +
            "ORDER BY an.submittedAt DESC, an.id DESC")
    List<AbsenceNotice> findNextPageBySessionIdsAndStatuses(
            @Param("sessionIds") List<UUID> sessionIds,
            @Param("statuses") List<AbsenceNoticeStatus> statuses,
            @Param("cursorSubmittedAt") LocalDateTime cursorSubmittedAt,
            @Param("cursorId") UUID cursorId
    );

    @Query("SELECT an FROM AbsenceNotice an WHERE an.studentId = :studentId " +
            "AND an.submittedAt >= :from AND an.submittedAt <= :to " +
            "ORDER BY an.submittedAt DESC, an.id DESC")
    List<AbsenceNotice> findFirstPageByStudentId(
            @Param("studentId") UUID studentId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    @Query("SELECT an FROM AbsenceNotice an WHERE an.studentId = :studentId " +
            "AND an.submittedAt >= :from AND an.submittedAt <= :to " +
            "AND ((an.submittedAt < :cursorSubmittedAt) OR (an.submittedAt = :cursorSubmittedAt AND an.id < :cursorId)) " +
            "ORDER BY an.submittedAt DESC, an.id DESC")
    List<AbsenceNotice> findNextPageByStudentId(
            @Param("studentId") UUID studentId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("cursorSubmittedAt") LocalDateTime cursorSubmittedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );
}
