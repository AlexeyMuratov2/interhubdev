package com.example.interhubdev.academic;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public API for Academic module: academic years and semesters.
 */
public interface AcademicApi {

    // --- Academic Year ---
    Optional<AcademicYearDto> findAcademicYearById(UUID id);

    Optional<AcademicYearDto> findCurrentAcademicYear();

    List<AcademicYearDto> findAllAcademicYears();

    AcademicYearDto createAcademicYear(String name, LocalDate startDate, LocalDate endDate, boolean isCurrent);

    AcademicYearDto updateAcademicYear(UUID id, String name, LocalDate startDate, LocalDate endDate, Boolean isCurrent);

    void deleteAcademicYear(UUID id);

    // --- Semester ---
    Optional<SemesterDto> findSemesterById(UUID id);

    Optional<SemesterDto> findCurrentSemester();

    /**
     * Find semester that contains the given date (startDate <= date <= endDate).
     *
     * @param date date to check
     * @return optional semester DTO if a semester exists for that date
     */
    Optional<SemesterDto> findSemesterByDate(LocalDate date);

    List<SemesterDto> findSemestersByAcademicYearId(UUID academicYearId);

    SemesterDto createSemester(
            UUID academicYearId,
            int number,
            String name,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate examStartDate,
            LocalDate examEndDate,
            Integer weekCount,
            boolean isCurrent
    );

    SemesterDto updateSemester(
            UUID id,
            String name,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate examStartDate,
            LocalDate examEndDate,
            Integer weekCount,
            Boolean isCurrent
    );

    void deleteSemester(UUID id);
}
