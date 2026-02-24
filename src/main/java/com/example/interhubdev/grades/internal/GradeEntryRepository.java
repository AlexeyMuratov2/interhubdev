package com.example.interhubdev.grades.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * JPA repository for grade_entry.
 */
interface GradeEntryRepository extends JpaRepository<GradeEntryEntity, UUID> {

    List<GradeEntryEntity> findByStudentIdAndOfferingIdOrderByGradedAtDesc(
            UUID studentId,
            UUID offeringId
    );

    /** from/to must be non-null (use sentinels in service when optional); avoids PostgreSQL parameter type inference with IS NULL. */
    @Query("SELECT e FROM GradeEntryEntity e WHERE e.studentId = :studentId AND e.offeringId = :offeringId " +
            "AND e.gradedAt >= :from AND e.gradedAt <= :to ORDER BY e.gradedAt DESC")
    List<GradeEntryEntity> findByStudentIdAndOfferingIdAndGradedAtBetween(
            @Param("studentId") UUID studentId,
            @Param("offeringId") UUID offeringId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    List<GradeEntryEntity> findByOfferingIdAndStudentIdInOrderByStudentIdAscGradedAtDesc(
            UUID offeringId,
            List<UUID> studentIds
    );

    /**
     * All ACTIVE grade entries linked to this lesson (for lesson-level points summary).
     */
    List<GradeEntryEntity> findByLessonIdAndStatusOrderByStudentIdAsc(
            UUID lessonId,
            String status
    );

    /**
     * ACTIVE grade entries linked to this lesson with no homework submission (lesson points only, excludes homework).
     * Used for lesson roster "points for this lesson" so homework grades are not mixed with lesson grades.
     */
    List<GradeEntryEntity> findByLessonIdAndHomeworkSubmissionIdIsNullAndStatusOrderByStudentIdAsc(
            UUID lessonId,
            String status
    );

    /**
     * ACTIVE grade entries for one student linked to this lesson with no homework submission (for set/replace lesson points).
     */
    List<GradeEntryEntity> findByLessonIdAndStudentIdAndHomeworkSubmissionIdIsNullAndStatusOrderByGradedAtDesc(
            UUID lessonId,
            UUID studentId,
            String status
    );

    /**
     * ACTIVE grade entries for one student linked to this lesson (for set/replace points).
     */
    List<GradeEntryEntity> findByLessonIdAndStudentIdAndStatusOrderByGradedAtDesc(
            UUID lessonId,
            UUID studentId,
            String status
    );

    /**
     * ACTIVE grade entries linked to any of the given homework submission IDs.
     * Used by composition to resolve points per submission in one query.
     */
    List<GradeEntryEntity> findByHomeworkSubmissionIdInAndStatus(
            List<UUID> submissionIds,
            String status
    );
}
