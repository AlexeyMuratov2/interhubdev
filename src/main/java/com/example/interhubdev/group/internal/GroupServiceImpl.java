package com.example.interhubdev.group.internal;

import com.example.interhubdev.group.*;
import com.example.interhubdev.student.StudentApi;
import com.example.interhubdev.student.StudentDto;
import com.example.interhubdev.user.UserApi;
import com.example.interhubdev.user.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class GroupServiceImpl implements GroupApi {

    private final GroupCatalogService groupCatalogService;
    private final GroupLeaderService groupLeaderService;
    private final GroupOverrideService groupOverrideService;
    private final StudentApi studentApi;
    private final UserApi userApi;

    @Override
    public Optional<StudentGroupDto> findGroupById(UUID id) {
        return groupCatalogService.findGroupById(id);
    }

    @Override
    public Optional<StudentGroupDto> findGroupByCode(String code) {
        return groupCatalogService.findGroupByCode(code);
    }

    @Override
    public List<StudentGroupDto> findAllGroups() {
        return groupCatalogService.findAllGroups();
    }

    @Override
    public List<StudentGroupDto> findGroupsByProgramId(UUID programId) {
        return groupCatalogService.findGroupsByProgramId(programId);
    }

    @Override
    @Transactional
    public StudentGroupDto createGroup(
            UUID programId,
            UUID curriculumId,
            String code,
            String name,
            String description,
            int startYear,
            Integer graduationYear,
            UUID curatorUserId
    ) {
        return groupCatalogService.createGroup(programId, curriculumId, code, name, description, startYear, graduationYear, curatorUserId);
    }

    @Override
    @Transactional
    public StudentGroupDto updateGroup(
            UUID id,
            String name,
            String description,
            Integer graduationYear,
            UUID curatorUserId
    ) {
        return groupCatalogService.updateGroup(id, name, description, graduationYear, curatorUserId);
    }

    @Override
    @Transactional
    public void deleteGroup(UUID id) {
        groupCatalogService.deleteGroup(id);
    }

    @Override
    public List<GroupMemberDto> getGroupMembersWithUsers(UUID groupId) {
        List<StudentDto> students = studentApi.findByGroupId(groupId);
        if (students.isEmpty()) return List.of();
        List<UUID> userIds = students.stream().map(StudentDto::userId).distinct().toList();
        List<UserDto> users = userApi.findByIds(userIds);
        Map<UUID, UserDto> userMap = users.stream().collect(Collectors.toMap(UserDto::id, u -> u));
        return students.stream()
                .map(s -> new GroupMemberDto(s, userMap.get(s.userId())))
                .toList();
    }

    @Override
    public List<GroupLeaderDetailDto> findLeadersByGroupId(UUID groupId) {
        return groupLeaderService.findLeadersByGroupId(groupId);
    }

    @Override
    @Transactional
    public GroupLeaderDto addGroupLeader(UUID groupId, UUID studentId, String role, LocalDate fromDate, LocalDate toDate) {
        return groupLeaderService.addGroupLeader(groupId, studentId, role, fromDate, toDate);
    }

    @Override
    @Transactional
    public void removeGroupLeader(UUID id) {
        groupLeaderService.removeGroupLeader(id);
    }

    @Override
    public List<GroupCurriculumOverrideDto> findOverridesByGroupId(UUID groupId) {
        return groupOverrideService.findOverridesByGroupId(groupId);
    }

    @Override
    @Transactional
    public GroupCurriculumOverrideDto createOverride(
            UUID groupId,
            UUID curriculumSubjectId,
            UUID subjectId,
            String action,
            UUID newAssessmentTypeId,
            Integer newDurationWeeks,
            String reason
    ) {
        return groupOverrideService.createOverride(groupId, curriculumSubjectId, subjectId, action, newAssessmentTypeId, newDurationWeeks, reason);
    }

    @Override
    @Transactional
    public void deleteOverride(UUID id) {
        groupOverrideService.deleteOverride(id);
    }
}
