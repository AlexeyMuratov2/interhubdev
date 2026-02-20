package com.example.interhubdev.academic.internal;

import com.example.interhubdev.academic.AcademicApi;
import com.example.interhubdev.academic.AcademicYearDto;
import com.example.interhubdev.academic.SemesterDto;
import com.example.interhubdev.error.Errors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class AcademicServiceImpl implements AcademicApi {

    private final AcademicYearRepository academicYearRepository;
    private final SemesterRepository semesterRepository;

    // --- Academic Year ---

    @Override
    public Optional<AcademicYearDto> findAcademicYearById(UUID id) {
        return academicYearRepository.findById(id).map(this::toAcademicYearDto);
    }

    @Override
    public Optional<AcademicYearDto> findCurrentAcademicYear() {
        return academicYearRepository.findByIsCurrent(true).map(this::toAcademicYearDto);
    }

    @Override
    public List<AcademicYearDto> findAllAcademicYears() {
        return academicYearRepository.findAllByOrderByStartDateDesc().stream()
                .map(this::toAcademicYearDto)
                .toList();
    }

    @Override
    @Transactional
    public AcademicYearDto createAcademicYear(String name, LocalDate startDate, LocalDate endDate, boolean isCurrent) {
        if (name == null || name.isBlank()) {
            throw Errors.badRequest("Academic year name is required");
        }
        if (startDate == null || endDate == null) {
            throw Errors.badRequest("Start date and end date are required");
        }
        if (!endDate.isAfter(startDate)) {
            throw Errors.badRequest("End date must be after start date");
        }
        String trimmedName = name.trim();
        if (academicYearRepository.existsByName(trimmedName)) {
            throw Errors.conflict("Academic year with name '" + trimmedName + "' already exists");
        }
        if (isCurrent) {
            clearCurrentAcademicYear();
        }
        AcademicYear entity = AcademicYear.builder()
                .name(trimmedName)
                .startDate(startDate)
                .endDate(endDate)
                .isCurrent(isCurrent)
                .build();
        return toAcademicYearDto(academicYearRepository.save(entity));
    }

    @Override
    @Transactional
    public AcademicYearDto updateAcademicYear(UUID id, String name, LocalDate startDate, LocalDate endDate, Boolean isCurrent) {
        AcademicYear entity = academicYearRepository.findById(id)
                .orElseThrow(() -> Errors.notFound("Academic year not found: " + id));
        if (name != null) entity.setName(name.trim());
        if (startDate != null) entity.setStartDate(startDate);
        if (endDate != null) entity.setEndDate(endDate);
        if (entity.getEndDate() != null && entity.getStartDate() != null && !entity.getEndDate().isAfter(entity.getStartDate())) {
            throw Errors.badRequest("End date must be after start date");
        }
        if (isCurrent != null && isCurrent && !entity.isCurrent()) {
            clearCurrentAcademicYear();
            entity.setCurrent(true);
        } else if (isCurrent != null) {
            entity.setCurrent(isCurrent);
        }
        return toAcademicYearDto(academicYearRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteAcademicYear(UUID id) {
        if (!academicYearRepository.existsById(id)) {
            throw Errors.notFound("Academic year not found: " + id);
        }
        academicYearRepository.deleteById(id);
    }

    private void clearCurrentAcademicYear() {
        academicYearRepository.findByIsCurrent(true).ifPresent(current -> {
            current.setCurrent(false);
            academicYearRepository.save(current);
        });
    }

    // --- Semester ---

    @Override
    public Optional<SemesterDto> findSemesterById(UUID id) {
        return semesterRepository.findById(id).map(this::toSemesterDto);
    }

    @Override
    public Optional<SemesterDto> findCurrentSemester() {
        return semesterRepository.findByIsCurrent(true).map(this::toSemesterDto);
    }

    @Override
    public Optional<SemesterDto> findSemesterByDate(LocalDate date) {
        if (date == null) {
            return Optional.empty();
        }
        return semesterRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(date, date)
                .map(this::toSemesterDto);
    }

    @Override
    public List<SemesterDto> findSemestersByAcademicYearId(UUID academicYearId) {
        return semesterRepository.findByAcademicYearIdOrderByNumberAsc(academicYearId).stream()
                .map(this::toSemesterDto)
                .toList();
    }

    @Override
    public Optional<SemesterDto> findSemesterByAcademicYearIdAndNumber(UUID academicYearId, int number) {
        if (number < 1 || number > 2) {
            return Optional.empty();
        }
        return semesterRepository.findByAcademicYearIdAndNumber(academicYearId, number)
                .map(this::toSemesterDto);
    }

    @Override
    public Optional<SemesterDto> findSemesterByCalendarYearAndNumber(int calendarYear, int semesterNo) {
        if (semesterNo < 1 || semesterNo > 2) {
            return Optional.empty();
        }
        // For groups: find academic year that contains the calendar year
        // Use date appropriate for the semester:
        // - Semester 1 typically starts in September (fall semester)
        // - Semester 2 typically starts in February (spring semester)
        // Try dates that are likely to be within the academic year
        java.time.LocalDate[] datesToTry = {
            java.time.LocalDate.of(calendarYear, semesterNo == 1 ? 9 : 2, 15), // Typical semester start
            java.time.LocalDate.of(calendarYear, 6, 15), // Mid-year fallback
            java.time.LocalDate.of(calendarYear, 1, 1) // Start of year fallback
        };
        
        for (java.time.LocalDate date : datesToTry) {
            Optional<AcademicYear> academicYear = academicYearRepository.findFirstByDate(date);
            if (academicYear.isPresent()) {
                Optional<SemesterDto> semester = semesterRepository.findByAcademicYearIdAndNumber(academicYear.get().getId(), semesterNo)
                        .map(this::toSemesterDto);
                if (semester.isPresent()) {
                    return semester;
                }
            }
        }
        
        // Fallback: academic year that starts in the given calendar year
        java.time.LocalDate yearStart = java.time.LocalDate.of(calendarYear, 1, 1);
        java.time.LocalDate yearEnd = java.time.LocalDate.of(calendarYear + 1, 1, 1);
        return academicYearRepository.findFirstByStartYear(yearStart, yearEnd)
                .flatMap(ay -> semesterRepository.findByAcademicYearIdAndNumber(ay.getId(), semesterNo))
                .map(this::toSemesterDto);
    }

    @Override
    @Transactional
    public SemesterDto createSemester(
            UUID academicYearId,
            int number,
            String name,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate examStartDate,
            LocalDate examEndDate,
            Integer weekCount,
            boolean isCurrent
    ) {
        if (academicYearId == null) {
            throw Errors.badRequest("Academic year id is required");
        }
        AcademicYear academicYear = academicYearRepository.findById(academicYearId)
                .orElseThrow(() -> Errors.notFound("Academic year not found: " + academicYearId));
        if (startDate == null || endDate == null) {
            throw Errors.badRequest("Start date and end date are required");
        }
        if (!endDate.isAfter(startDate)) {
            throw Errors.badRequest("End date must be after start date");
        }
        if (number < 1 || number > 2) {
            throw Errors.badRequest("Semester number must be 1 or 2");
        }
        if (weekCount != null && (weekCount < 1 || weekCount > 52)) {
            throw Errors.badRequest("Week count must be between 1 and 52");
        }
        if (startDate.isBefore(academicYear.getStartDate())) {
            throw Errors.badRequest("Semester start date must be on or after academic year start date");
        }
        if (endDate.isAfter(academicYear.getEndDate())) {
            throw Errors.badRequest("Semester end date must be on or before academic year end date");
        }
        if (semesterRepository.existsByAcademicYearIdAndNumber(academicYearId, number)) {
            throw Errors.conflict("Semester " + number + " already exists for this academic year");
        }
        if (isCurrent) {
            clearCurrentSemester();
        }
        Semester entity = Semester.builder()
                .academicYearId(academicYearId)
                .number(number)
                .name(name != null ? name.trim() : null)
                .startDate(startDate)
                .endDate(endDate)
                .examStartDate(examStartDate)
                .examEndDate(examEndDate)
                .weekCount(weekCount != null ? weekCount : 16)
                .isCurrent(isCurrent)
                .build();
        return toSemesterDto(semesterRepository.save(entity));
    }

    @Override
    @Transactional
    public SemesterDto updateSemester(
            UUID id,
            String name,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate examStartDate,
            LocalDate examEndDate,
            Integer weekCount,
            Boolean isCurrent
    ) {
        Semester entity = semesterRepository.findById(id)
                .orElseThrow(() -> Errors.notFound("Semester not found: " + id));
        if (name != null) entity.setName(name.trim());
        if (startDate != null) entity.setStartDate(startDate);
        if (endDate != null) entity.setEndDate(endDate);
        if (entity.getEndDate() != null && entity.getStartDate() != null && !entity.getEndDate().isAfter(entity.getStartDate())) {
            throw Errors.badRequest("End date must be after start date");
        }
        AcademicYear academicYear = academicYearRepository.findById(entity.getAcademicYearId())
                .orElseThrow(() -> Errors.notFound("Academic year not found: " + entity.getAcademicYearId()));
        if (entity.getStartDate().isBefore(academicYear.getStartDate()) || entity.getEndDate().isAfter(academicYear.getEndDate())) {
            throw Errors.badRequest("Semester dates must fall within academic year range");
        }
        if (weekCount != null && (weekCount < 1 || weekCount > 52)) {
            throw Errors.badRequest("Week count must be between 1 and 52");
        }
        if (examStartDate != null) entity.setExamStartDate(examStartDate);
        if (examEndDate != null) entity.setExamEndDate(examEndDate);
        if (weekCount != null) entity.setWeekCount(weekCount);
        if (isCurrent != null && isCurrent && !entity.isCurrent()) {
            clearCurrentSemester();
            entity.setCurrent(true);
        } else if (isCurrent != null) {
            entity.setCurrent(isCurrent);
        }
        return toSemesterDto(semesterRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteSemester(UUID id) {
        if (!semesterRepository.existsById(id)) {
            throw Errors.notFound("Semester not found: " + id);
        }
        semesterRepository.deleteById(id);
    }

    private void clearCurrentSemester() {
        semesterRepository.findByIsCurrent(true).ifPresent(current -> {
            current.setCurrent(false);
            semesterRepository.save(current);
        });
    }

    // --- Mappers ---

    private AcademicYearDto toAcademicYearDto(AcademicYear e) {
        return new AcademicYearDto(e.getId(), e.getName(), e.getStartDate(), e.getEndDate(),
                e.isCurrent(), e.getCreatedAt());
    }

    private SemesterDto toSemesterDto(Semester e) {
        return new SemesterDto(e.getId(), e.getAcademicYearId(), e.getNumber(), e.getName(),
                e.getStartDate(), e.getEndDate(), e.getExamStartDate(), e.getExamEndDate(),
                e.getWeekCount(), e.isCurrent(), e.getCreatedAt());
    }
}
