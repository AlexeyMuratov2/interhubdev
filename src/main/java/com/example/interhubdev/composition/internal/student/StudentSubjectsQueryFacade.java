package com.example.interhubdev.composition.internal.student;

import com.example.interhubdev.composition.StudentSubjectInfoDto;
import com.example.interhubdev.composition.StudentSubjectsDto;
import com.example.interhubdev.composition.StudentSubjectsQueryApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Facade implementing StudentSubjectsQueryApi; delegates to per-use-case services.
 */
@Service
@RequiredArgsConstructor
class StudentSubjectsQueryFacade implements StudentSubjectsQueryApi {

    private final StudentSubjectsService studentSubjectsService;
    private final StudentSubjectInfoService studentSubjectInfoService;

    @Override
    public StudentSubjectsDto getStudentSubjects(UUID requesterId, Optional<Integer> semesterNo) {
        return studentSubjectsService.execute(requesterId, semesterNo);
    }

    @Override
    public StudentSubjectInfoDto getStudentSubjectInfo(UUID offeringId, UUID requesterId, Optional<UUID> semesterId) {
        return studentSubjectInfoService.execute(offeringId, requesterId, semesterId);
    }
}
