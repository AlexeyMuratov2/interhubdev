package com.example.interhubdev.program.internal;

import com.example.interhubdev.error.Errors;
import com.example.interhubdev.program.CurriculumSubjectDto;
import com.example.interhubdev.subject.SubjectApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class CurriculumSubjectService {

    private final CurriculumRepository curriculumRepository;
    private final CurriculumSubjectRepository curriculumSubjectRepository;
    private final SubjectApi subjectApi;

    Optional<CurriculumSubjectDto> findCurriculumSubjectById(UUID id) {
        return curriculumSubjectRepository.findById(id).map(ProgramMappers::toCurriculumSubjectDto);
    }

    /**
     * Batch load subject display names by curriculum subject ids. Name: englishName or chineseName or code.
     */
    Map<UUID, String> getSubjectNamesByCurriculumSubjectIds(List<UUID> curriculumSubjectIds) {
        if (curriculumSubjectIds == null || curriculumSubjectIds.isEmpty()) {
            return Map.of();
        }
        List<CurriculumSubject> curriculumSubjects = curriculumSubjectRepository.findAllById(curriculumSubjectIds);
        if (curriculumSubjects.isEmpty()) {
            return Map.of();
        }
        List<UUID> subjectIds = curriculumSubjects.stream().map(CurriculumSubject::getSubjectId).distinct().toList();
        List<com.example.interhubdev.subject.SubjectDto> subjects = subjectApi.findSubjectsByIds(subjectIds);
        Map<UUID, String> subjectIdToName = subjects.stream()
                .collect(Collectors.toMap(
                        com.example.interhubdev.subject.SubjectDto::id,
                        s -> subjectDisplayName(s)
                ));
        return curriculumSubjects.stream()
                .collect(Collectors.toMap(
                        CurriculumSubject::getId,
                        cs -> subjectIdToName.getOrDefault(cs.getSubjectId(), "")
                ));
    }

    private static String subjectDisplayName(com.example.interhubdev.subject.SubjectDto s) {
        if (s.englishName() != null && !s.englishName().isBlank()) return s.englishName();
        if (s.chineseName() != null && !s.chineseName().isBlank()) return s.chineseName();
        return s.code() != null ? s.code() : "";
    }

    List<CurriculumSubjectDto> findCurriculumSubjectsByCurriculumId(UUID curriculumId) {
        return curriculumSubjectRepository.findByCurriculumIdOrderBySemesterNoAscSubjectIdAsc(curriculumId).stream()
                .map(ProgramMappers::toCurriculumSubjectDto)
                .toList();
    }

    @Transactional
    CurriculumSubjectDto createCurriculumSubject(
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
        if (curriculumId == null) throw Errors.badRequest("Curriculum id is required");
        if (subjectId == null) throw Errors.badRequest("Subject id is required");
        if (assessmentTypeId == null) throw Errors.badRequest("Assessment type id is required");

        if (curriculumRepository.findById(curriculumId).isEmpty()) {
            throw Errors.notFound("Curriculum not found: " + curriculumId);
        }
        if (subjectApi.findSubjectById(subjectId).isEmpty()) {
            throw Errors.notFound("Subject not found: " + subjectId);
        }
        if (subjectApi.findAssessmentTypeById(assessmentTypeId).isEmpty()) {
            throw Errors.notFound("Assessment type not found: " + assessmentTypeId);
        }

        ProgramValidation.validatePositive(semesterNo, "semesterNo");
        ProgramValidation.validatePositive(durationWeeks, "durationWeeks");

        // Common-sense checks; keep them permissive for now.
        ProgramValidation.validateNonNegative(hoursTotal, "hoursTotal");
        ProgramValidation.validateNonNegative(hoursLecture, "hoursLecture");
        ProgramValidation.validateNonNegative(hoursPractice, "hoursPractice");
        ProgramValidation.validateNonNegative(hoursLab, "hoursLab");
        ProgramValidation.validateNonNegative(hoursSeminar, "hoursSeminar");
        ProgramValidation.validateNonNegative(hoursSelfStudy, "hoursSelfStudy");
        ProgramValidation.validateNonNegative(hoursConsultation, "hoursConsultation");
        ProgramValidation.validateNonNegative(hoursCourseWork, "hoursCourseWork");

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

        return ProgramMappers.toCurriculumSubjectDto(curriculumSubjectRepository.save(entity));
    }

    @Transactional
    CurriculumSubjectDto updateCurriculumSubject(
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

        ProgramValidation.validateNonNegative(hoursTotal, "hoursTotal");
        ProgramValidation.validateNonNegative(hoursLecture, "hoursLecture");
        ProgramValidation.validateNonNegative(hoursPractice, "hoursPractice");
        ProgramValidation.validateNonNegative(hoursLab, "hoursLab");
        ProgramValidation.validateNonNegative(hoursSeminar, "hoursSeminar");
        ProgramValidation.validateNonNegative(hoursSelfStudy, "hoursSelfStudy");
        ProgramValidation.validateNonNegative(hoursConsultation, "hoursConsultation");
        ProgramValidation.validateNonNegative(hoursCourseWork, "hoursCourseWork");

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
        return ProgramMappers.toCurriculumSubjectDto(curriculumSubjectRepository.save(entity));
    }

    @Transactional
    void deleteCurriculumSubject(UUID id) {
        if (!curriculumSubjectRepository.existsById(id)) {
            throw Errors.notFound("Curriculum subject not found: " + id);
        }
        curriculumSubjectRepository.deleteById(id);
    }
}

