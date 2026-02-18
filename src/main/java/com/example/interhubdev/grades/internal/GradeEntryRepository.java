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

    @Query("SELECT e FROM GradeEntryEntity e WHERE e.studentId = :studentId AND e.offeringId = :offeringId " +
            "AND (:from IS NULL OR e.gradedAt >= :from) AND (:to IS NULL OR e.gradedAt <= :to) " +
            "ORDER BY e.gradedAt DESC")
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
}
