package com.example.interhubdev.program;

import java.util.Optional;
import java.util.UUID;

/**
 * Port for resolving a calendar semester by year and number.
 * Implemented by the adapter using the Academic module so that Program does not depend on Academic.
 *
 * @see com.example.interhubdev.adapter.SemesterIdByYearAdapter
 */
public interface SemesterIdByYearPort {

    /**
     * Find semester ID by calendar year of the academic year start and semester number (1 or 2).
     *
     * @param calendarYear calendar year of the academic year's start date (e.g. 2024 for "2024/25")
     * @param semesterNo   semester number within the year (1 or 2)
     * @return optional semester ID if found
     */
    Optional<UUID> findSemesterIdByCalendarYearAndNumber(int calendarYear, int semesterNo);
}
