package com.example.interhubdev.program.internal;

import com.example.interhubdev.department.DepartmentApi;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.program.CurriculumDto;
import com.example.interhubdev.program.CurriculumPracticeDto;
import com.example.interhubdev.program.CurriculumStatus;
import com.example.interhubdev.program.CurriculumSubjectAssessmentDto;
import com.example.interhubdev.program.CurriculumSubjectDto;
import com.example.interhubdev.program.PracticeLocation;
import com.example.interhubdev.program.PracticeType;
import com.example.interhubdev.program.ProgramApi;
import com.example.interhubdev.program.ProgramDto;
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
    private final CurriculumSubjectAssessmentRepository curriculumSubjectAssessmentRepository;
    private final CurriculumPracticeRepository curriculumPracticeRepository;
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
    public CurriculumDto createCurriculum(UUID programId, String version, int startYear, Integer endYear, boolean isActive, String notes) {
        if (programRepository.findById(programId).isEmpty()) {
            throw Errors.notFound("Program not found: " + programId);
        }
        if (version == null || version.isBlank()) {
            throw Errors.badRequest("Curriculum version is required");
        }
        if (startYear < MIN_YEAR || startYear > MAX_YEAR) {
            throw Errors.badRequest("startYear must be between " + MIN_YEAR + " and " + MAX_YEAR);
        }
        if (endYear != null && (endYear < MIN_YEAR || endYear > MAX_YEAR)) {
            throw Errors.badRequest("endYear must be between " + MIN_YEAR + " and " + MAX_YEAR);
        }
        if (endYear != null && endYear < startYear) {
            throw Errors.badRequest("endYear must be greater than or equal to startYear");
        }
        String trimmedVersion = version.trim();
        if (curriculumRepository.existsByProgramIdAndVersion(programId, trimmedVersion)) {
            throw Errors.conflict("Curriculum with version '" + trimmedVersion + "' already exists for program");
        }
        Curriculum entity = Curriculum.builder()
                .programId(programId)
                .version(trimmedVersion)
                .startYear(startYear)
                .endYear(endYear)
                .isActive(isActive)
                .notes(notes != null ? notes.trim() : null)
                .build();
        return toCurriculumDto(curriculumRepository.save(entity));
    }

    @Override
    @Transactional
    public CurriculumDto updateCurriculum(UUID id, String version, int startYear, Integer endYear, boolean isActive, CurriculumStatus status, String notes) {
        Curriculum entity = curriculumRepository.findById(id)
                .orElseThrow(() -> Errors.notFound("Curriculum not found: " + id));
        if (version != null) entity.setVersion(version.trim());
        if (startYear < MIN_YEAR || startYear > MAX_YEAR) {
            throw Errors.badRequest("startYear must be between " + MIN_YEAR + " and " + MAX_YEAR);
        }
        if (endYear != null && (endYear < MIN_YEAR || endYear > MAX_YEAR)) {
            throw Errors.badRequest("endYear must be between " + MIN_YEAR + " and " + MAX_YEAR);
        }
        if (endYear != null && endYear < startYear) {
            throw Errors.badRequest("endYear must be greater than or equal to startYear");
        }
        entity.setStartYear(startYear);
        entity.setEndYear(endYear);
        entity.setActive(isActive);
        if (status != null) entity.setStatus(status);
        if (notes != null) entity.setNotes(notes.trim());
        entity.setUpdatedAt(LocalDateTime.now());
        return toCurriculumDto(curriculumRepository.save(entity));
    }

    @Override
    @Transactional
    public CurriculumDto approveCurriculum(UUID id, UUID approvedBy) {
        Curriculum entity = curriculumRepository.findById(id)
                .orElseThrow(() -> Errors.notFound("Curriculum not found: " + id));
        entity.setStatus(CurriculumStatus.APPROVED);
        entity.setApprovedAt(LocalDateTime.now());
        entity.setApprovedBy(approvedBy);
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
            Integer hoursSeminar,
            Integer hoursSelfStudy,
            Integer hoursConsultation,
            Integer hoursCourseWork,
            UUID assessmentTypeId,
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
                .hoursSeminar(hoursSeminar)
                .hoursSelfStudy(hoursSelfStudy)
                .hoursConsultation(hoursConsultation)
                .hoursCourseWork(hoursCourseWork)
                .assessmentTypeId(assessmentTypeId)
                .isElective(false) // Electives not supported
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
            Integer hoursSeminar,
            Integer hoursSelfStudy,
            Integer hoursConsultation,
            Integer hoursCourseWork,
            UUID assessmentTypeId,
            BigDecimal credits
    ) {
        CurriculumSubject entity = curriculumSubjectRepository.findById(id)
                .orElseThrow(() -> Errors.notFound("Curriculum subject not found: " + id));
        if (courseYear != null) entity.setCourseYear(courseYear);
        if (hoursTotal != null) entity.setHoursTotal(hoursTotal);
        if (hoursLecture != null) entity.setHoursLecture(hoursLecture);
        if (hoursPractice != null) entity.setHoursPractice(hoursPractice);
        if (hoursLab != null) entity.setHoursLab(hoursLab);
        if (hoursSeminar != null) entity.setHoursSeminar(hoursSeminar);
        if (hoursSelfStudy != null) entity.setHoursSelfStudy(hoursSelfStudy);
        if (hoursConsultation != null) entity.setHoursConsultation(hoursConsultation);
        if (hoursCourseWork != null) entity.setHoursCourseWork(hoursCourseWork);
        if (assessmentTypeId != null) {
            if (subjectApi.findAssessmentTypeById(assessmentTypeId).isEmpty()) {
                throw Errors.notFound("Assessment type not found: " + assessmentTypeId);
            }
            entity.setAssessmentTypeId(assessmentTypeId);
        }
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

    // --- Curriculum subject assessment ---

    @Override
    public List<CurriculumSubjectAssessmentDto> findAssessmentsByCurriculumSubjectId(UUID curriculumSubjectId) {
        return curriculumSubjectAssessmentRepository.findByCurriculumSubjectId(curriculumSubjectId).stream()
                .map(this::toCurriculumSubjectAssessmentDto)
                .toList();
    }

    @Override
    @Transactional
    public CurriculumSubjectAssessmentDto createCurriculumSubjectAssessment(
            UUID curriculumSubjectId,
            UUID assessmentTypeId,
            Integer weekNumber,
            boolean isFinal,
            BigDecimal weight,
            String notes
    ) {
        if (curriculumSubjectId == null) {
            throw Errors.badRequest("Curriculum subject id is required");
        }
        if (assessmentTypeId == null) {
            throw Errors.badRequest("Assessment type id is required");
        }
        if (curriculumSubjectRepository.findById(curriculumSubjectId).isEmpty()) {
            throw Errors.notFound("Curriculum subject not found: " + curriculumSubjectId);
        }
        if (subjectApi.findAssessmentTypeById(assessmentTypeId).isEmpty()) {
            throw Errors.notFound("Assessment type not found: " + assessmentTypeId);
        }
        if (weight != null && (weight.compareTo(BigDecimal.ZERO) < 0 || weight.compareTo(BigDecimal.ONE) > 0)) {
            throw Errors.badRequest("Weight must be between 0 and 1");
        }
        CurriculumSubjectAssessment entity = CurriculumSubjectAssessment.builder()
                .curriculumSubjectId(curriculumSubjectId)
                .assessmentTypeId(assessmentTypeId)
                .weekNumber(weekNumber)
                .isFinal(isFinal)
                .weight(weight)
                .notes(notes != null ? notes.trim() : null)
                .build();
        return toCurriculumSubjectAssessmentDto(curriculumSubjectAssessmentRepository.save(entity));
    }

    @Override
    @Transactional
    public CurriculumSubjectAssessmentDto updateCurriculumSubjectAssessment(
            UUID id,
            UUID assessmentTypeId,
            Integer weekNumber,
            Boolean isFinal,
            BigDecimal weight,
            String notes
    ) {
        CurriculumSubjectAssessment entity = curriculumSubjectAssessmentRepository.findById(id)
                .orElseThrow(() -> Errors.notFound("Curriculum subject assessment not found: " + id));
        if (assessmentTypeId != null) {
            if (subjectApi.findAssessmentTypeById(assessmentTypeId).isEmpty()) {
                throw Errors.notFound("Assessment type not found: " + assessmentTypeId);
            }
            entity.setAssessmentTypeId(assessmentTypeId);
        }
        if (weekNumber != null) entity.setWeekNumber(weekNumber);
        if (isFinal != null) entity.setFinal(isFinal);
        if (weight != null) {
            if (weight.compareTo(BigDecimal.ZERO) < 0 || weight.compareTo(BigDecimal.ONE) > 0) {
                throw Errors.badRequest("Weight must be between 0 and 1");
            }
            entity.setWeight(weight);
        }
        if (notes != null) entity.setNotes(notes.trim());
        return toCurriculumSubjectAssessmentDto(curriculumSubjectAssessmentRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteCurriculumSubjectAssessment(UUID id) {
        if (!curriculumSubjectAssessmentRepository.existsById(id)) {
            throw Errors.notFound("Curriculum subject assessment not found: " + id);
        }
        curriculumSubjectAssessmentRepository.deleteById(id);
    }

    // --- Curriculum practice ---

    @Override
    public List<CurriculumPracticeDto> findPracticesByCurriculumId(UUID curriculumId) {
        return curriculumPracticeRepository.findByCurriculumId(curriculumId).stream()
                .map(this::toCurriculumPracticeDto)
                .toList();
    }

    @Override
    @Transactional
    public CurriculumPracticeDto createCurriculumPractice(
            UUID curriculumId,
            PracticeType practiceType,
            String name,
            String description,
            int semesterNo,
            int durationWeeks,
            BigDecimal credits,
            UUID assessmentTypeId,
            PracticeLocation locationType,
            boolean supervisorRequired,
            boolean reportRequired,
            String notes
    ) {
        if (curriculumId == null) {
            throw Errors.badRequest("Curriculum id is required");
        }
        if (practiceType == null) {
            throw Errors.badRequest("Practice type is required");
        }
        if (name == null || name.isBlank()) {
            throw Errors.badRequest("Practice name is required");
        }
        if (curriculumRepository.findById(curriculumId).isEmpty()) {
            throw Errors.notFound("Curriculum not found: " + curriculumId);
        }
        if (assessmentTypeId != null && subjectApi.findAssessmentTypeById(assessmentTypeId).isEmpty()) {
            throw Errors.notFound("Assessment type not found: " + assessmentTypeId);
        }
        if (semesterNo < 1) {
            throw Errors.badRequest("semesterNo must be >= 1");
        }
        if (durationWeeks < 1) {
            throw Errors.badRequest("durationWeeks must be >= 1");
        }
        CurriculumPractice entity = CurriculumPractice.builder()
                .curriculumId(curriculumId)
                .practiceType(practiceType)
                .name(name.trim())
                .description(description != null ? description.trim() : null)
                .semesterNo(semesterNo)
                .durationWeeks(durationWeeks)
                .credits(credits)
                .assessmentTypeId(assessmentTypeId)
                .locationType(locationType != null ? locationType : PracticeLocation.ENTERPRISE)
                .supervisorRequired(supervisorRequired)
                .reportRequired(reportRequired)
                .notes(notes != null ? notes.trim() : null)
                .build();
        return toCurriculumPracticeDto(curriculumPracticeRepository.save(entity));
    }

    @Override
    @Transactional
    public CurriculumPracticeDto updateCurriculumPractice(
            UUID id,
            PracticeType practiceType,
            String name,
            String description,
            Integer semesterNo,
            Integer durationWeeks,
            BigDecimal credits,
            UUID assessmentTypeId,
            PracticeLocation locationType,
            Boolean supervisorRequired,
            Boolean reportRequired,
            String notes
    ) {
        CurriculumPractice entity = curriculumPracticeRepository.findById(id)
                .orElseThrow(() -> Errors.notFound("Curriculum practice not found: " + id));
        if (practiceType != null) entity.setPracticeType(practiceType);
        if (name != null) entity.setName(name.trim());
        if (description != null) entity.setDescription(description.trim());
        if (semesterNo != null) {
            if (semesterNo < 1) {
                throw Errors.badRequest("semesterNo must be >= 1");
            }
            entity.setSemesterNo(semesterNo);
        }
        if (durationWeeks != null) {
            if (durationWeeks < 1) {
                throw Errors.badRequest("durationWeeks must be >= 1");
            }
            entity.setDurationWeeks(durationWeeks);
        }
        if (credits != null) entity.setCredits(credits);
        if (assessmentTypeId != null) {
            if (subjectApi.findAssessmentTypeById(assessmentTypeId).isEmpty()) {
                throw Errors.notFound("Assessment type not found: " + assessmentTypeId);
            }
            entity.setAssessmentTypeId(assessmentTypeId);
        }
        if (locationType != null) entity.setLocationType(locationType);
        if (supervisorRequired != null) entity.setSupervisorRequired(supervisorRequired);
        if (reportRequired != null) entity.setReportRequired(reportRequired);
        if (notes != null) entity.setNotes(notes.trim());
        entity.setUpdatedAt(LocalDateTime.now());
        return toCurriculumPracticeDto(curriculumPracticeRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteCurriculumPractice(UUID id) {
        if (!curriculumPracticeRepository.existsById(id)) {
            throw Errors.notFound("Curriculum practice not found: " + id);
        }
        curriculumPracticeRepository.deleteById(id);
    }

    private ProgramDto toProgramDto(Program e) {
        return new ProgramDto(e.getId(), e.getCode(), e.getName(), e.getDescription(),
                e.getDegreeLevel(), e.getDepartmentId(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private CurriculumDto toCurriculumDto(Curriculum e) {
        return new CurriculumDto(e.getId(), e.getProgramId(), e.getVersion(), e.getStartYear(),
                e.getEndYear(), e.isActive(), e.getStatus(), e.getApprovedAt(), e.getApprovedBy(),
                e.getNotes(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private CurriculumSubjectDto toCurriculumSubjectDto(CurriculumSubject e) {
        return new CurriculumSubjectDto(e.getId(), e.getCurriculumId(), e.getSubjectId(), e.getSemesterNo(),
                e.getCourseYear(), e.getDurationWeeks(), e.getHoursTotal(), e.getHoursLecture(),
                e.getHoursPractice(), e.getHoursLab(), e.getHoursSeminar(), e.getHoursSelfStudy(),
                e.getHoursConsultation(), e.getHoursCourseWork(), e.getAssessmentTypeId(),
                e.getCredits(), e.getCreatedAt(), e.getUpdatedAt());
    }

    private CurriculumSubjectAssessmentDto toCurriculumSubjectAssessmentDto(CurriculumSubjectAssessment e) {
        return new CurriculumSubjectAssessmentDto(e.getId(), e.getCurriculumSubjectId(), e.getAssessmentTypeId(),
                e.getWeekNumber(), e.isFinal(), e.getWeight(), e.getNotes(), e.getCreatedAt());
    }

    private CurriculumPracticeDto toCurriculumPracticeDto(CurriculumPractice e) {
        return new CurriculumPracticeDto(e.getId(), e.getCurriculumId(), e.getPracticeType(), e.getName(),
                e.getDescription(), e.getSemesterNo(), e.getDurationWeeks(), e.getCredits(),
                e.getAssessmentTypeId(), e.getLocationType(), e.isSupervisorRequired(),
                e.isReportRequired(), e.getNotes(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
