package com.example.interhubdev.composition.internal.shared;

import com.example.interhubdev.program.ProgramApi;
import com.example.interhubdev.subject.SubjectApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Shared resolver: curriculum subject ID → display name (english preferred, fallback to chinese).
 * Use in composition use-case services to avoid duplicating subject name resolution.
 */
@Component
@RequiredArgsConstructor
public class SubjectNameResolver {

    private final ProgramApi programApi;
    private final SubjectApi subjectApi;

    /**
     * Resolve display name for a curriculum subject.
     *
     * @param curriculumSubjectId curriculum subject ID (must not be null)
     * @return subject display name, or empty string if not found
     */
    public String resolve(UUID curriculumSubjectId) {
        return programApi.findCurriculumSubjectById(curriculumSubjectId)
                .flatMap(cs -> subjectApi.findSubjectById(cs.subjectId()))
                .map(s -> s.englishName() != null && !s.englishName().isBlank() ? s.englishName() : s.chineseName())
                .orElse("");
    }
}
