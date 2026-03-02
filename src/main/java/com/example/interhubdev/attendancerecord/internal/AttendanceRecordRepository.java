package com.example.interhubdev.attendancerecord.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for attendance_record.
 */
interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, UUID> {

    List<AttendanceRecord> findByLessonSessionId(UUID lessonSessionId);

    Optional<AttendanceRecord> findByLessonSessionIdAndStudentId(UUID lessonSessionId, UUID studentId);

    List<AttendanceRecord> findByStudentIdOrderByMarkedAtDesc(UUID studentId);

    List<AttendanceRecord> findByStudentIdAndMarkedAtGreaterThanEqualOrderByMarkedAtDesc(UUID studentId, LocalDateTime from);

    List<AttendanceRecord> findByStudentIdAndMarkedAtLessThanEqualOrderByMarkedAtDesc(UUID studentId, LocalDateTime to);

    List<AttendanceRecord> findByStudentIdAndMarkedAtBetweenOrderByMarkedAtDesc(UUID studentId, LocalDateTime from, LocalDateTime to);

    List<AttendanceRecord> findByLessonSessionIdIn(List<UUID> lessonSessionIds);

    @Query("SELECT ar FROM AttendanceRecord ar WHERE ar.studentId = :studentId " +
            "AND ar.lessonSessionId IN :sessionIds ORDER BY ar.markedAt DESC")
    List<AttendanceRecord> findByStudentIdAndLessonSessionIdIn(
            @Param("studentId") UUID studentId,
            @Param("sessionIds") List<UUID> sessionIds
    );

    @Query("SELECT ar FROM AttendanceRecord ar WHERE ar.studentId = :studentId " +
            "AND ar.lessonSessionId IN :sessionIds " +
            "AND (:from IS NULL OR ar.markedAt >= :from) " +
            "AND (:to IS NULL OR ar.markedAt <= :to) " +
            "ORDER BY ar.markedAt DESC")
    List<AttendanceRecord> findByStudentIdAndLessonSessionIdInAndMarkedAtBetween(
            @Param("studentId") UUID studentId,
            @Param("sessionIds") List<UUID> sessionIds,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    List<AttendanceRecord> findByAbsenceNoticeId(UUID absenceNoticeId);
}
