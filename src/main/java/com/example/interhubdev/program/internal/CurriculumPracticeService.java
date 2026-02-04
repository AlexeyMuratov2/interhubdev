package com.example.interhubdev.program.internal;

import com.example.interhubdev.error.Errors;
import com.example.interhubdev.program.CurriculumPracticeDto;
import com.example.interhubdev.program.PracticeLocation;
import com.example.interhubdev.program.PracticeType;
import com.example.interhubdev.subject.SubjectApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class CurriculumPracticeService {

    private final CurriculumRepository curriculumRepository;
    private final CurriculumPracticeRepository curriculumPracticeRepository;
    private final SubjectApi subjectApi;

    List<CurriculumPracticeDto> findPracticesByCurriculumId(UUID curriculumId) {
        return curriculumPracticeRepository.findByCurriculumIdOrderBySemesterNoAscNameAsc(curriculumId).stream()
                .map(ProgramMappers::toCurriculumPracticeDto)
                .toList();
    }

    @Transactional
    CurriculumPracticeDto createCurriculumPractice(
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
        if (curriculumId == null) throw Errors.badRequest("Curriculum id is required");
        if (practiceType == null) throw Errors.badRequest("Practice type is required");
        String trimmedName = ProgramValidation.requiredTrimmed(name, "Practice name");

        if (curriculumRepository.findById(curriculumId).isEmpty()) {
            throw Errors.notFound("Curriculum not found: " + curriculumId);
        }
        if (assessmentTypeId != null && subjectApi.findAssessmentTypeById(assessmentTypeId).isEmpty()) {
            throw Errors.notFound("Assessment type not found: " + assessmentTypeId);
        }
        ProgramValidation.validatePositive(semesterNo, "semesterNo");
        ProgramValidation.validatePositive(durationWeeks, "durationWeeks");

        CurriculumPractice entity = CurriculumPractice.builder()
                .curriculumId(curriculumId)
                .practiceType(practiceType)
                .name(trimmedName)
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
        return ProgramMappers.toCurriculumPracticeDto(curriculumPracticeRepository.save(entity));
    }

    @Transactional
    CurriculumPracticeDto updateCurriculumPractice(
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
            ProgramValidation.validatePositive(semesterNo, "semesterNo");
            entity.setSemesterNo(semesterNo);
        }
        if (durationWeeks != null) {
            ProgramValidation.validatePositive(durationWeeks, "durationWeeks");
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
        return ProgramMappers.toCurriculumPracticeDto(curriculumPracticeRepository.save(entity));
    }

    @Transactional
    void deleteCurriculumPractice(UUID id) {
        if (!curriculumPracticeRepository.existsById(id)) {
            throw Errors.notFound("Curriculum practice not found: " + id);
        }
        curriculumPracticeRepository.deleteById(id);
    }
}

