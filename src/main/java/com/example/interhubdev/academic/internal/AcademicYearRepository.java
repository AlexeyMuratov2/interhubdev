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

    /** Academic year that contains the given calendar year (e.g. 2026 finds "2025/26" if it spans into 2026). */
    @Query("SELECT ay FROM AcademicYear ay WHERE ay.startDate <= :date AND ay.endDate >= :date")
    Optional<AcademicYear> findFirstByDate(@Param("date") java.time.LocalDate date);

    /** Academic year that starts in the given calendar year (e.g. 2024 finds "2024/25" if it starts in 2024). */
    @Query("SELECT ay FROM AcademicYear ay WHERE ay.startDate >= :yearStart AND ay.startDate < :yearEnd ORDER BY ay.startDate DESC")
    Optional<AcademicYear> findFirstByStartYear(@Param("yearStart") java.time.LocalDate yearStart, @Param("yearEnd") java.time.LocalDate yearEnd);
}
