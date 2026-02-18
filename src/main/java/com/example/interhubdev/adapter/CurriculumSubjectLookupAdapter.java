package com.example.interhubdev.adapter;

import com.example.interhubdev.offering.CurriculumSubjectLookupPort;
import com.example.interhubdev.program.CurriculumSubjectDto;
import com.example.interhubdev.program.ProgramApi;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter: implements Offering's CurriculumSubjectLookupPort using Program module's ProgramApi.
 * Uses @Lazy to break circular dependency between offering and program modules.
 */
@Component
public class CurriculumSubjectLookupAdapter implements CurriculumSubjectLookupPort {

    private final ProgramApi programApi;

    public CurriculumSubjectLookupAdapter(@Lazy ProgramApi programApi) {
        this.programApi = programApi;
    }

    @Override
    public Optional<CurriculumSubjectDto> findById(UUID id) {
        return programApi.findCurriculumSubjectById(id);
    }

    @Override
    public Map<UUID, String> getSubjectNamesByCurriculumSubjectIds(List<UUID> curriculumSubjectIds) {
        return programApi.getSubjectNamesByCurriculumSubjectIds(curriculumSubjectIds);
    }
}
