package com.example.interhubdev.academic.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface SemesterRepository extends JpaRepository<Semester, UUID> {

    List<Semester> findByAcademicYearId(UUID academicYearId);

    /** Semesters for the given year ordered by number ascending. */
    List<Semester> findByAcademicYearIdOrderByNumberAsc(UUID academicYearId);

    Optional<Semester> findByIsCurrent(boolean isCurrent);

    boolean existsByAcademicYearIdAndNumber(UUID academicYearId, int number);
}
