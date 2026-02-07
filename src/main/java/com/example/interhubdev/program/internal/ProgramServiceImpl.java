package com.example.interhubdev.program.internal;

import com.example.interhubdev.program.CurriculumDto;
import com.example.interhubdev.program.CurriculumPracticeDto;
import com.example.interhubdev.program.CurriculumStatus;
import com.example.interhubdev.program.CurriculumSubjectAssessmentDto;
import com.example.interhubdev.program.CurriculumSubjectDto;
import com.example.interhubdev.program.PracticeLocation;
import com.example.interhubdev.program.PracticeType;
import com.example.interhubdev.program.ProgramApi;
import com.example.interhubdev.program.ProgramDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class ProgramServiceImpl implements ProgramApi {

    private final ProgramCatalogService programCatalogService;
    private final CurriculumService curriculumService;
    private final CurriculumSubjectService curriculumSubjectService;
    private final CurriculumAssessmentService curriculumAssessmentService;
    private final CurriculumPracticeService curriculumPracticeService;

    @Override
    public Optional<ProgramDto> findProgramById(UUID id) {
        return programCatalogService.findProgramById(id);
    }

    @Override
    public Optional<ProgramDto> findProgramByCode(String code) {
        return programCatalogService.findProgramByCode(code);
    }

    @Override
    public List<ProgramDto> findAllPrograms() {
        return programCatalogService.findAllPrograms();
    }

    @Override
    @Transactional
    public ProgramDto createProgram(String code, String name, String description, String degreeLevel, UUID departmentId) {
        return programCatalogService.createProgram(code, name, description, degreeLevel, departmentId);
    }

    @Override
    @Transactional
    public ProgramDto updateProgram(UUID id, String name, String description, String degreeLevel, UUID departmentId) {
        return programCatalogService.updateProgram(id, name, description, degreeLevel, departmentId);
    }

    @Override
    @Transactional
    public void deleteProgram(UUID id) {
        programCatalogService.deleteProgram(id);
    }

    @Override
    public Optional<CurriculumDto> findCurriculumById(UUID id) {
        return curriculumService.findCurriculumById(id);
    }

    @Override
    public List<CurriculumDto> findCurriculaByProgramId(UUID programId) {
        return curriculumService.findCurriculaByProgramId(programId);
    }

    @Override
    @Transactional
    public CurriculumDto createCurriculum(UUID programId, String version, int startYear, Integer endYear, boolean isActive, String notes) {
        return curriculumService.createCurriculum(programId, version, startYear, endYear, isActive, notes);
    }

    @Override
    @Transactional
    public CurriculumDto updateCurriculum(UUID id, String version, int startYear, Integer endYear, boolean isActive, CurriculumStatus status, String notes) {
        return curriculumService.updateCurriculum(id, version, startYear, endYear, isActive, status, notes);
    }

    @Override
    @Transactional
    public CurriculumDto approveCurriculum(UUID id, UUID approvedBy) {
        return curriculumService.approveCurriculum(id, approvedBy);
    }

    @Override
    @Transactional
    public void deleteCurriculum(UUID id) {
        curriculumService.deleteCurriculum(id);
    }

    @Override
    public Optional<CurriculumSubjectDto> findCurriculumSubjectById(UUID id) {
        return curriculumSubjectService.findCurriculumSubjectById(id);
    }

    @Override
    public Map<UUID, String> getSubjectNamesByCurriculumSubjectIds(List<UUID> curriculumSubjectIds) {
        return curriculumSubjectService.getSubjectNamesByCurriculumSubjectIds(curriculumSubjectIds);
    }

    @Override
    public List<CurriculumSubjectDto> findCurriculumSubjectsByCurriculumId(UUID curriculumId) {
        return curriculumSubjectService.findCurriculumSubjectsByCurriculumId(curriculumId);
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
        return curriculumSubjectService.createCurriculumSubject(curriculumId, subjectId, semesterNo, courseYear,
                durationWeeks, hoursTotal, hoursLecture, hoursPractice, hoursLab, hoursSeminar,
                hoursSelfStudy, hoursConsultation, hoursCourseWork, assessmentTypeId, credits);
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
        return curriculumSubjectService.updateCurriculumSubject(id, courseYear, hoursTotal, hoursLecture, hoursPractice,
                hoursLab, hoursSeminar, hoursSelfStudy, hoursConsultation, hoursCourseWork, assessmentTypeId, credits);
    }

    @Override
    @Transactional
    public void deleteCurriculumSubject(UUID id) {
        curriculumSubjectService.deleteCurriculumSubject(id);
    }

    // --- Curriculum subject assessment ---

    @Override
    public List<CurriculumSubjectAssessmentDto> findAssessmentsByCurriculumSubjectId(UUID curriculumSubjectId) {
        return curriculumAssessmentService.findAssessmentsByCurriculumSubjectId(curriculumSubjectId);
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
        return curriculumAssessmentService.createCurriculumSubjectAssessment(curriculumSubjectId, assessmentTypeId, weekNumber, isFinal, weight, notes);
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
        return curriculumAssessmentService.updateCurriculumSubjectAssessment(id, assessmentTypeId, weekNumber, isFinal, weight, notes);
    }

    @Override
    @Transactional
    public void deleteCurriculumSubjectAssessment(UUID id) {
        curriculumAssessmentService.deleteCurriculumSubjectAssessment(id);
    }

    // --- Curriculum practice ---

    @Override
    public List<CurriculumPracticeDto> findPracticesByCurriculumId(UUID curriculumId) {
        return curriculumPracticeService.findPracticesByCurriculumId(curriculumId);
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
        return curriculumPracticeService.createCurriculumPractice(curriculumId, practiceType, name, description, semesterNo,
                durationWeeks, credits, assessmentTypeId, locationType, supervisorRequired, reportRequired, notes);
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
        return curriculumPracticeService.updateCurriculumPractice(id, practiceType, name, description, semesterNo,
                durationWeeks, credits, assessmentTypeId, locationType, supervisorRequired, reportRequired, notes);
    }

    @Override
    @Transactional
    public void deleteCurriculumPractice(UUID id) {
        curriculumPracticeService.deleteCurriculumPractice(id);
    }
}
