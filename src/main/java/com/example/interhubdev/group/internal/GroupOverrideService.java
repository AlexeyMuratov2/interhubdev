package com.example.interhubdev.group.internal;

import com.example.interhubdev.error.Errors;
import com.example.interhubdev.group.GroupCurriculumOverrideDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class GroupOverrideService {

    private final StudentGroupRepository studentGroupRepository;
    private final GroupCurriculumOverrideRepository overrideRepository;

    List<GroupCurriculumOverrideDto> findOverridesByGroupId(UUID groupId) {
        return overrideRepository.findByGroupIdOrderByCreatedAtDesc(groupId).stream()
                .map(GroupMappers::toOverrideDto)
                .toList();
    }

    @Transactional
    GroupCurriculumOverrideDto createOverride(
            UUID groupId,
            UUID curriculumSubjectId,
            UUID subjectId,
            String action,
            UUID newAssessmentTypeId,
            Integer newDurationWeeks,
            String reason
    ) {
        String normalizedAction = GroupValidation.normalizeOverrideAction(action);

        if (studentGroupRepository.findById(groupId).isEmpty()) {
            throw Errors.notFound("Group not found: " + groupId);
        }

        if ("REMOVE".equals(normalizedAction) && curriculumSubjectId == null) {
            throw Errors.badRequest("curriculumSubjectId is required for REMOVE");
        }
        if ("ADD".equals(normalizedAction) && subjectId == null) {
            throw Errors.badRequest("subjectId is required for ADD");
        }
        if ("REPLACE".equals(normalizedAction) && curriculumSubjectId == null) {
            throw Errors.badRequest("curriculumSubjectId is required for REPLACE");
        }

        GroupCurriculumOverride entity = GroupCurriculumOverride.builder()
                .groupId(groupId)
                .curriculumSubjectId(curriculumSubjectId)
                .subjectId(subjectId)
                .action(normalizedAction)
                .newAssessmentTypeId(newAssessmentTypeId)
                .newDurationWeeks(newDurationWeeks)
                .reason(reason != null ? reason.trim() : null)
                .build();
        return GroupMappers.toOverrideDto(overrideRepository.save(entity));
    }

    @Transactional
    void deleteOverride(UUID id) {
        if (!overrideRepository.existsById(id)) {
            throw Errors.notFound("Override not found: " + id);
        }
        overrideRepository.deleteById(id);
    }
}

