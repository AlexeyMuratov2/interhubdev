package com.example.interhubdev.adapter;

import com.example.interhubdev.academic.AcademicApi;
import com.example.interhubdev.program.SemesterIdByYearPort;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter: implements Program's SemesterIdByYearPort using Academic module's AcademicApi.
 * Allows Program to resolve semester ID by calendar year and number without depending on Academic.
 */
@Component
public class SemesterIdByYearAdapter implements SemesterIdByYearPort {

    private final AcademicApi academicApi;

    public SemesterIdByYearAdapter(AcademicApi academicApi) {
        this.academicApi = academicApi;
    }

    @Override
    public Optional<UUID> findSemesterIdByCalendarYearAndNumber(int calendarYear, int semesterNo) {
        return academicApi.findSemesterByCalendarYearAndNumber(calendarYear, semesterNo)
                .map(dto -> dto.id());
    }
}
