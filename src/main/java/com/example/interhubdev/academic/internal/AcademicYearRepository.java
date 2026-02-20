package com.example.interhubdev.academic.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface AcademicYearRepository extends JpaRepository<AcademicYear, UUID> {

    Optional<AcademicYear> findByIsCurrent(boolean isCurrent);

    boolean existsByName(String name);

    /** All academic years ordered by start date descending (most recent first). */
    List<AcademicYear> findAllByOrderByStartDateDesc();

    /** Academic year whose start date falls in the given calendar year (e.g. 2024 for "2024/25"). */
    @Query("SELECT ay FROM AcademicYear ay WHERE YEAR(ay.startDate) = :year")
    Optional<AcademicYear> findFirstByStartDateYear(@Param("year") int year);
}
