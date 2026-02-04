package com.example.interhubdev.program.internal;

import com.example.interhubdev.error.Errors;
import com.example.interhubdev.program.CurriculumSubjectAssessmentDto;
import com.example.interhubdev.subject.SubjectApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class CurriculumAssessmentService {

    private final CurriculumSubjectRepository curriculumSubjectRepository;
    private final CurriculumSubjectAssessmentRepository curriculumSubjectAssessmentRepository;
    private final SubjectApi subjectApi;

    List<CurriculumSubjectAssessmentDto> findAssessmentsByCurriculumSubjectId(UUID curriculumSubjectId) {
        return curriculumSubjectAssessmentRepository.findByCurriculumSubjectId(curriculumSubjectId).stream()
                .sorted(Comparator
                        .comparing(CurriculumSubjectAssessment::isFinal).reversed()
                        .thenComparing(a -> a.getWeekNumber() != null ? a.getWeekNumber() : Integer.MAX_VALUE)
                        .thenComparing(CurriculumSubjectAssessment::getCreatedAt))
                .map(ProgramMappers::toCurriculumSubjectAssessmentDto)
                .toList();
    }

    @Transactional
    CurriculumSubjectAssessmentDto createCurriculumSubjectAssessment(
            UUID curriculumSubjectId,
            UUID assessmentTypeId,
            Integer weekNumber,
            boolean isFinal,
            BigDecimal weight,
            String notes
    ) {
        if (curriculumSubjectId == null) throw Errors.badRequest("Curriculum subject id is required");
        if (assessmentTypeId == null) throw Errors.badRequest("Assessment type id is required");

        if (curriculumSubjectRepository.findById(curriculumSubjectId).isEmpty()) {
            throw Errors.notFound("Curriculum subject not found: " + curriculumSubjectId);
        }
        if (subjectApi.findAssessmentTypeById(assessmentTypeId).isEmpty()) {
            throw Errors.notFound("Assessment type not found: " + assessmentTypeId);
        }
        ProgramValidation.validateWeight01(weight);

        CurriculumSubjectAssessment entity = CurriculumSubjectAssessment.builder()
                .curriculumSubjectId(curriculumSubjectId)
                .assessmentTypeId(assessmentTypeId)
                .weekNumber(weekNumber)
                .isFinal(isFinal)
                .weight(weight)
                .notes(notes != null ? notes.trim() : null)
                .build();
        return ProgramMappers.toCurriculumSubjectAssessmentDto(curriculumSubjectAssessmentRepository.save(entity));
    }

    @Transactional
    CurriculumSubjectAssessmentDto updateCurriculumSubjectAssessment(
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
            ProgramValidation.validateWeight01(weight);
            entity.setWeight(weight);
        }
        if (notes != null) entity.setNotes(notes.trim());
        return ProgramMappers.toCurriculumSubjectAssessmentDto(curriculumSubjectAssessmentRepository.save(entity));
    }

    @Transactional
    void deleteCurriculumSubjectAssessment(UUID id) {
        if (!curriculumSubjectAssessmentRepository.existsById(id)) {
            throw Errors.notFound("Curriculum subject assessment not found: " + id);
        }
        curriculumSubjectAssessmentRepository.deleteById(id);
    }
}

