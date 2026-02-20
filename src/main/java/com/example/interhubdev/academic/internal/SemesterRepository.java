package com.example.interhubdev.academic.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SemesterRepository extends JpaRepository<Semester, UUID> {

    List<Semester> findByAcademicYearId(UUID academicYearId);

    /** Semesters for the given year ordered by number ascending. */
    List<Semester> findByAcademicYearIdOrderByNumberAsc(UUID academicYearId);

    /** Single semester by academic year and number (1 or 2). */
    Optional<Semester> findByAcademicYearIdAndNumber(UUID academicYearId, int number);

    Optional<Semester> findByIsCurrent(boolean isCurrent);

    /** Semester that contains the given date (startDate <= date <= endDate). */
    Optional<Semester> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate date, LocalDate sameDate);

    boolean existsByAcademicYearIdAndNumber(UUID academicYearId, int number);
}
