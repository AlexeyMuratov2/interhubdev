package com.example.interhubdev.group;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public API for Group module: student groups, group leaders, curriculum overrides.
 */
public interface GroupApi {

    // --- Student group ---
    Optional<StudentGroupDto> findGroupById(UUID id);

    Optional<StudentGroupDto> findGroupByCode(String code);

    List<StudentGroupDto> findAllGroups();

    List<StudentGroupDto> findGroupsByProgramId(UUID programId);

    StudentGroupDto createGroup(
            UUID programId,
            UUID curriculumId,
            String code,
            String name,
            String description,
            int startYear,
            Integer graduationYear,
            UUID curatorTeacherId
    );

    StudentGroupDto updateGroup(
            UUID id,
            String name,
            String description,
            Integer graduationYear,
            UUID curatorTeacherId
    );

    void deleteGroup(UUID id);

    // --- Group leader ---
    List<GroupLeaderDto> findLeadersByGroupId(UUID groupId);

    GroupLeaderDto addGroupLeader(UUID groupId, UUID studentId, String role, java.time.LocalDate fromDate, java.time.LocalDate toDate);

    void removeGroupLeader(UUID id);

    // --- Group curriculum override ---
    List<GroupCurriculumOverrideDto> findOverridesByGroupId(UUID groupId);

    GroupCurriculumOverrideDto createOverride(
            UUID groupId,
            UUID curriculumSubjectId,
            UUID subjectId,
            String action,
            UUID newAssessmentTypeId,
            Integer newDurationWeeks,
            String reason
    );

    void deleteOverride(UUID id);
}
