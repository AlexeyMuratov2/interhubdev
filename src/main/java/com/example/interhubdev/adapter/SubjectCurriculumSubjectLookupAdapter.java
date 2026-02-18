package com.example.interhubdev.adapter;

import com.example.interhubdev.program.CurriculumSubjectAssessmentDto;
import com.example.interhubdev.program.CurriculumSubjectDto;
import com.example.interhubdev.program.ProgramApi;
import com.example.interhubdev.subject.CurriculumSubjectLookupPort;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter: implements Subject's CurriculumSubjectLookupPort using Program module's ProgramApi.
 * Uses @Lazy to break circular dependency between subject and program modules.
 */
@Component
public class SubjectCurriculumSubjectLookupAdapter implements CurriculumSubjectLookupPort {

    @Lazy
    private final ProgramApi programApi;

    public SubjectCurriculumSubjectLookupAdapter(@Lazy ProgramApi programApi) {
        this.programApi = programApi;
    }

    @Override
    public Optional<CurriculumSubjectDto> findById(UUID id) {
        return programApi.findCurriculumSubjectById(id);
    }

    @Override
    public List<CurriculumSubjectAssessmentDto> findAssessmentsByCurriculumSubjectId(UUID curriculumSubjectId) {
        return programApi.findAssessmentsByCurriculumSubjectId(curriculumSubjectId);
    }
}
