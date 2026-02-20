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

    /**
     * Find semester by academic year and number (1 or 2).
     *
     * @param academicYearId academic year ID (identifies the year)
     * @param number         semester number within the year (1 or 2)
     * @return optional semester DTO if found
     */
    Optional<SemesterDto> findSemesterByAcademicYearIdAndNumber(UUID academicYearId, int number);

    /**
     * Find semester by calendar year of the academic year start and semester number (1 or 2).
     * E.g. calendarYear 2024 finds the academic year that starts in 2024 (e.g. "2024/25"), then semester by number.
     *
     * @param calendarYear calendar year of the academic year's start date
     * @param semesterNo   semester number within the year (1 or 2)
     * @return optional semester DTO if found
     */
    Optional<SemesterDto> findSemesterByCalendarYearAndNumber(int calendarYear, int semesterNo);

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
