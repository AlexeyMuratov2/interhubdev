package com.example.interhubdev.academic;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Public API for Academic module: academic years and semesters.
 */
public interface AcademicApi {

    // --- Academic Year ---
    Optional<AcademicYearDto> findAcademicYearById(UUID id);

    /**
     * Find academic years by IDs. Missing IDs are skipped; result may be smaller than input size.
     *
     * @param ids academic year IDs (empty collection returns empty list)
     * @return list of academic year DTOs for found IDs
     */
    List<AcademicYearDto> findAcademicYearsByIds(Collection<UUID> ids);

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

    /**
     * Find distinct semesters that contain any of the given dates (startDate &lt;= date &lt;= endDate).
     * Used when resolving semesters for a set of lesson dates (e.g. composition teacher student groups).
     *
     * @param dates dates to resolve (empty set returns empty list)
     * @return list of distinct semester DTOs, order not specified
     */
    List<SemesterDto> findSemestersByDates(Set<LocalDate> dates);

    /**
     * Find semesters by IDs. Missing IDs are skipped; result may be smaller than input size.
     *
     * @param ids semester IDs (empty collection returns empty list)
     * @return list of semester DTOs for found IDs
     */
    List<SemesterDto> findSemestersByIds(Collection<UUID> ids);

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
