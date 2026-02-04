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
        return academicYearRepository.findAll().stream()
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
    public List<SemesterDto> findSemestersByAcademicYearId(UUID academicYearId) {
        return semesterRepository.findByAcademicYearId(academicYearId).stream()
                .map(this::toSemesterDto)
                .toList();
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
        if (academicYearRepository.findById(academicYearId).isEmpty()) {
            throw Errors.notFound("Academic year not found: " + academicYearId);
        }
        if (startDate == null || endDate == null) {
            throw Errors.badRequest("Start date and end date are required");
        }
        if (!endDate.isAfter(startDate)) {
            throw Errors.badRequest("End date must be after start date");
        }
        if (number < 1) {
            throw Errors.badRequest("Semester number must be >= 1");
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
