package com.example.interhubdev.attendance.internal;

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

    /**
     * Find all attendance records for a lesson session.
     */
    List<AttendanceRecord> findByLessonSessionId(UUID lessonSessionId);

    /**
     * Find attendance record for a specific student and session.
     */
    Optional<AttendanceRecord> findByLessonSessionIdAndStudentId(UUID lessonSessionId, UUID studentId);

    /**
     * Find all attendance records for a student (no date filter).
     * Used when both from and to are null to avoid PostgreSQL "could not determine data type of parameter" with optional params.
     */
    List<AttendanceRecord> findByStudentIdOrderByMarkedAtDesc(UUID studentId);

    /**
     * Find all attendance records for a student with markedAt &gt;= from.
     */
    List<AttendanceRecord> findByStudentIdAndMarkedAtGreaterThanEqualOrderByMarkedAtDesc(UUID studentId, LocalDateTime from);

    /**
     * Find all attendance records for a student with markedAt &lt;= to.
     */
    List<AttendanceRecord> findByStudentIdAndMarkedAtLessThanEqualOrderByMarkedAtDesc(UUID studentId, LocalDateTime to);

    /**
     * Find all attendance records for a student within a date range (inclusive).
     * Use this only when both from and to are non-null; otherwise use the other overloads to avoid PostgreSQL parameter type inference issues.
     */
    List<AttendanceRecord> findByStudentIdAndMarkedAtBetweenOrderByMarkedAtDesc(UUID studentId, LocalDateTime from, LocalDateTime to);

    /**
     * Find all attendance records for multiple lesson sessions.
     * Used for group summary queries.
     */
    List<AttendanceRecord> findByLessonSessionIdIn(List<UUID> lessonSessionIds);

    /**
     * Find attendance records for a student filtered by lesson session IDs (no date filter).
     * Call only when sessionIds is non-empty. Used by getStudentAttendanceByLessonIds to avoid
     * PostgreSQL parameter type inference issues with null from/to.
     */
    @Query("SELECT ar FROM AttendanceRecord ar WHERE ar.studentId = :studentId " +
            "AND ar.lessonSessionId IN :sessionIds ORDER BY ar.markedAt DESC")
    List<AttendanceRecord> findByStudentIdAndLessonSessionIdIn(
            @Param("studentId") UUID studentId,
            @Param("sessionIds") List<UUID> sessionIds
    );

    /**
     * Find attendance records for a student filtered by lesson session IDs and date range.
     */
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
}
