package com.example.interhubdev.program.internal;

import com.example.interhubdev.department.DepartmentApi;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.program.*;
import com.example.interhubdev.subject.SubjectApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class ProgramServiceImpl implements ProgramApi {

    private static final int MIN_YEAR = 1900;
    private static final int MAX_YEAR = 2100;

    private final ProgramRepository programRepository;
    private final CurriculumRepository curriculumRepository;
    private final CurriculumSubjectRepository curriculumSubjectRepository;
    private final DepartmentApi departmentApi;
    private final SubjectApi subjectApi;

    @Override
    public Optional<ProgramDto> findProgramById(UUID id) {
        return programRepository.findById(id).map(this::toProgramDto);
    }

    @Override
    public Optional<ProgramDto> findProgramByCode(String code) {
        return programRepository.findByCode(code).map(this::toProgramDto);
    }

    @Override
    public List<ProgramDto> findAllPrograms() {
        return programRepository.findAll().stream()
                .map(this::toProgramDto)
                .toList();
    }

    @Override
    @Transactional
    public ProgramDto createProgram(String code, String name, String description, String degreeLevel, UUID departmentId) {
        if (code == null || code.isBlank()) {
            throw Errors.badRequest("Program code is required");
        }
        String trimmedCode = code.trim();
        if (programRepository.existsByCode(trimmedCode)) {
            throw Errors.conflict("Program with code '" + trimmedCode + "' already exists");
        }
        if (departmentId != null && departmentApi.findById(departmentId).isEmpty()) {
            throw Errors.notFound("Department not found: " + departmentId);
        }
        Program entity = Program.builder()
                .code(trimmedCode)
                .name(name != null ? name.trim() : "")
                .description(description != null ? description.trim() : null)
                .degreeLevel(degreeLevel != null ? degreeLevel.trim() : null)
                .departmentId(departmentId)
                .build();
        return toProgramDto(programRepository.save(entity));
    }

    @Override
    @Transactional
    public ProgramDto updateProgram(UUID id, String name, String description, String degreeLevel, UUID departmentId) {
        Program entity = programRepository.findById(id)
                .orElseThrow(() -> Errors.notFound("Program not found: " + id));
        if (departmentId != null && departmentApi.findById(departmentId).isEmpty()) {
            throw Errors.notFound("Department not found: " + departmentId);
        }
        if (name != null) entity.setName(name.trim());
        if (description != null) entity.setDescription(description.trim());
        if (degreeLevel != null) entity.setDegreeLevel(degreeLevel.trim());
        entity.setDepartmentId(departmentId);
        entity.setUpdatedAt(LocalDateTime.now());
        return toProgramDto(programRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteProgram(UUID id) {
        if (!programRepository.existsById(id)) {
            throw Errors.notFound("Program not found: " + id);
        }
        programRepository.deleteById(id);
    }

    @Override
    public Optional<CurriculumDto> findCurriculumById(UUID id) {
        return curriculumRepository.findById(id).map(this::toCurriculumDto);
    }

    @Override
    public List<CurriculumDto> findCurriculaByProgramId(UUID programId) {
        return curriculumRepository.findByProgramId(programId).stream()
                .map(this::toCurriculumDto)
                .toList();
    }

    @Override
    @Transactional
    public CurriculumDto createCurriculum(UUID programId, String version, int startYear, boolean isActive, String notes) {
        if (programRepository.findById(programId).isEmpty()) {
            throw Errors.notFound("Program not found: " + programId);
        }
        if (version == null || version.isBlank()) {
            throw Errors.badRequest("Curriculum version is required");
        }
        if (startYear < MIN_YEAR || startYear > MAX_YEAR) {
            throw Errors.badRequest("startYear must be between " + MIN_YEAR + " and " + MAX_YEAR);
        }
        String trimmedVersion = version.trim();
        if (curriculumRepository.existsByProgramIdAndVersion(programId, trimmedVersion)) {
            throw Errors.conflict("Curriculum with version '" + trimmedVersion + "' already exists for program");
        }
        Curriculum entity = Curriculum.builder()
                .programId(programId)
                .version(trimmedVersion)
                .startYear(startYear)
                .isActive(isActive)
                .notes(notes != null ? notes.trim() : null)
                .build();
        return toCurriculumDto(curriculumRepository.save(entity));
    }

    @Override
    @Transactional
    public CurriculumDto updateCurriculum(UUID id, String version, int startYear, boolean isActive, String notes) {
        Curriculum entity = curriculumRepository.findById(id)
                .orElseThrow(() -> Errors.notFound("Curriculum not found: " + id));
        if (version != null) entity.setVersion(version.trim());
        if (startYear < MIN_YEAR || startYear > MAX_YEAR) {
            throw Errors.badRequest("startYear must be between " + MIN_YEAR + " and " + MAX_YEAR);
        }
        entity.setStartYear(startYear);
        entity.setActive(isActive);
        if (notes != null) entity.setNotes(notes.trim());
        entity.setUpdatedAt(LocalDateTime.now());
        return toCurriculumDto(curriculumRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteCurriculum(UUID id) {
        if (!curriculumRepository.existsById(id)) {
            throw Errors.notFound("Curriculum not found: " + id);
        }
        curriculumRepository.deleteById(id);
    }

    @Override
    public Optional<CurriculumSubjectDto> findCurriculumSubjectById(UUID id) {
        return curriculumSubjectRepository.findById(id).map(this::toCurriculumSubjectDto);
    }

    @Override
    public List<CurriculumSubjectDto> findCurriculumSubjectsByCurriculumId(UUID curriculumId) {
        return curriculumSubjectRepository.findByCurriculumId(curriculumId).stream()
                .map(this::toCurriculumSubjectDto)
                .toList();
    }

    @Override
    @Transactional
    public CurriculumSubjectDto createCurriculumSubject(
            UUID curriculumId,
            UUID subjectId,
            int semesterNo,
            Integer courseYear,
            int durationWeeks,
            Integer hoursTotal,
            Integer hoursLecture,
            Integer hoursPractice,
            Integer hoursLab,
            UUID assessmentTypeId,
            boolean isElective,
            BigDecimal credits
    ) {
        if (curriculumId == null) {
            throw Errors.badRequest("Curriculum id is required");
        }
        if (subjectId == null) {
            throw Errors.badRequest("Subject id is required");
        }
        if (assessmentTypeId == null) {
            throw Errors.badRequest("Assessment type id is required");
        }
        if (curriculumRepository.findById(curriculumId).isEmpty()) {
            throw Errors.notFound("Curriculum not found: " + curriculumId);
        }
        if (subjectApi.findSubjectById(subjectId).isEmpty()) {
            throw Errors.notFound("Subject not found: " + subjectId);
        }
        if (subjectApi.findAssessmentTypeById(assessmentTypeId).isEmpty()) {
            throw Errors.notFound("Assessment type not found: " + assessmentTypeId);
        }
        if (semesterNo < 1) {
            throw Errors.badRequest("semesterNo must be >= 1");
        }
        if (durationWeeks < 1) {
            throw Errors.badRequest("durationWeeks must be >= 1");
        }
        if (curriculumSubjectRepository.existsByCurriculumIdAndSubjectIdAndSemesterNo(curriculumId, subjectId, semesterNo)) {
            throw Errors.conflict("Curriculum subject already exists for curriculum, subject, semester " + semesterNo);
        }
        CurriculumSubject entity = CurriculumSubject.builder()
                .curriculumId(curriculumId)
                .subjectId(subjectId)
                .semesterNo(semesterNo)
                .courseYear(courseYear)
                .durationWeeks(durationWeeks)
                .hoursTotal(hoursTotal)
                .hoursLecture(hoursLecture)
                .hoursPractice(hoursPractice)
                .hoursLab(hoursLab)
                .assessmentTypeId(assessmentTypeId)
                .isElective(isElective)
                .credits(credits)
                .build();
        return toCurriculumSubjectDto(curriculumSubjectRepository.save(entity));
    }

    @Override
    @Transactional
    public CurriculumSubjectDto updateCurriculumSubject(
            UUID id,
            Integer courseYear,
            Integer hoursTotal,
            Integer hoursLecture,
            Integer hoursPractice,
            Integer hoursLab,
            UUID assessmentTypeId,
            Boolean isElective,
            BigDecimal credits
    ) {
        CurriculumSubject entity = curriculumSubjectRepository.findById(id)
                .orElseThrow(() -> Errors.notFound("Curriculum subject not found: " + id));
        if (courseYear != null) entity.setCourseYear(courseYear);
        if (hoursTotal != null) entity.setHoursTotal(hoursTotal);
        if (hoursLecture != null) entity.setHoursLecture(hoursLecture);
        if (hoursPractice != null) entity.setHoursPractice(hoursPractice);
        if (hoursLab != null) entity.setHoursLab(hoursLab);
        if (assessmentTypeId != null) {
            if (subjectApi.findAssessmentTypeById(assessmentTypeId).isEmpty()) {
                throw Errors.notFound("Assessment type not found: " + assessmentTypeId);
            }
            entity.setAssessmentTypeId(assessmentTypeId);
        }
        if (isElective != null) entity.setElective(isElective);
        if (credits != null) entity.setCredits(credits);
        entity.setUpdatedAt(LocalDateTime.now());
        return toCurriculumSubjectDto(curriculumSubjectRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteCurriculumSubject(UUID id) {
        if (!curriculumSubjectRepository.existsById(id)) {
            throw Errors.notFound("Curriculum subject not found: " + id);
        }
        curriculumSubjectRepository.deleteById(id);
    }

    private ProgramDto toProgramDto(Program e) {
        return new ProgramDto(e.getId(), e.getCode(), e.getName(), e.getDescription(),
                e.getDegreeLevel(), e.getDepartmentId(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private CurriculumDto toCurriculumDto(Curriculum e) {
        return new CurriculumDto(e.getId(), e.getProgramId(), e.getVersion(), e.getStartYear(),
                e.isActive(), e.getNotes(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private CurriculumSubjectDto toCurriculumSubjectDto(CurriculumSubject e) {
        return new CurriculumSubjectDto(e.getId(), e.getCurriculumId(), e.getSubjectId(), e.getSemesterNo(),
                e.getCourseYear(), e.getDurationWeeks(), e.getHoursTotal(), e.getHoursLecture(),
                e.getHoursPractice(), e.getHoursLab(), e.getAssessmentTypeId(), e.isElective(),
                e.getCredits(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
