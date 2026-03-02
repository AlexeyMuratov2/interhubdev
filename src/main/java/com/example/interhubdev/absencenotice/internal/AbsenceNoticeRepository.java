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
 * JPA repository for absence_notice. Lesson links are in absence_notice_lesson.
 */
interface AbsenceNoticeRepository extends JpaRepository<AbsenceNotice, UUID> {

    Optional<AbsenceNotice> findByIdAndStudentId(UUID id, UUID studentId);

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

    /**
     * Find notices by IDs, filtered by student and status, ordered by submittedAt desc (for last-submitted lookup).
     */
    @Query("SELECT an FROM AbsenceNotice an WHERE an.id IN :noticeIds AND an.studentId = :studentId AND an.status = :status " +
            "ORDER BY an.submittedAt DESC, an.updatedAt DESC")
    List<AbsenceNotice> findByIdInAndStudentIdAndStatusOrderBySubmittedAtDesc(
            @Param("noticeIds") List<UUID> noticeIds,
            @Param("studentId") UUID studentId,
            @Param("status") AbsenceNoticeStatus status,
            Pageable pageable
    );

    @Query("SELECT an FROM AbsenceNotice an WHERE an.id IN :noticeIds ORDER BY an.submittedAt DESC, an.id DESC")
    List<AbsenceNotice> findByIdInOrderBySubmittedAtDescIdDesc(@Param("noticeIds") List<UUID> noticeIds);

    @Query("SELECT an FROM AbsenceNotice an WHERE an.id IN :noticeIds AND an.status IN :statuses ORDER BY an.submittedAt DESC, an.id DESC")
    List<AbsenceNotice> findByIdInAndStatusInOrderBySubmittedAtDescIdDesc(
            @Param("noticeIds") List<UUID> noticeIds,
            @Param("statuses") List<AbsenceNoticeStatus> statuses
    );

    @Query("SELECT an FROM AbsenceNotice an WHERE an.id IN :noticeIds ORDER BY an.submittedAt DESC, an.id DESC")
    List<AbsenceNotice> findFirstPageByNoticeIds(@Param("noticeIds") List<UUID> noticeIds, Pageable pageable);

    @Query("SELECT an FROM AbsenceNotice an WHERE an.id IN :noticeIds AND " +
            "((an.submittedAt < :cursorSubmittedAt) OR (an.submittedAt = :cursorSubmittedAt AND an.id < :cursorId)) " +
            "ORDER BY an.submittedAt DESC, an.id DESC")
    List<AbsenceNotice> findNextPageByNoticeIds(
            @Param("noticeIds") List<UUID> noticeIds,
            @Param("cursorSubmittedAt") LocalDateTime cursorSubmittedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );

    @Query("SELECT an FROM AbsenceNotice an WHERE an.id IN :noticeIds AND an.status IN :statuses ORDER BY an.submittedAt DESC, an.id DESC")
    List<AbsenceNotice> findFirstPageByNoticeIdsAndStatuses(
            @Param("noticeIds") List<UUID> noticeIds,
            @Param("statuses") List<AbsenceNoticeStatus> statuses,
            Pageable pageable
    );

    @Query("SELECT an FROM AbsenceNotice an WHERE an.id IN :noticeIds AND an.status IN :statuses AND " +
            "((an.submittedAt < :cursorSubmittedAt) OR (an.submittedAt = :cursorSubmittedAt AND an.id < :cursorId)) " +
            "ORDER BY an.submittedAt DESC, an.id DESC")
    List<AbsenceNotice> findNextPageByNoticeIdsAndStatuses(
            @Param("noticeIds") List<UUID> noticeIds,
            @Param("statuses") List<AbsenceNoticeStatus> statuses,
            @Param("cursorSubmittedAt") LocalDateTime cursorSubmittedAt,
            @Param("cursorId") UUID cursorId,
            Pageable pageable
    );
}
