package com.example.interhubdev.group.internal;

import com.example.interhubdev.group.*;
import com.example.interhubdev.program.ProgramApi;
import com.example.interhubdev.student.StudentApi;
import com.example.interhubdev.teacher.TeacherApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class GroupServiceImpl implements GroupApi {

    private final StudentGroupRepository studentGroupRepository;
    private final GroupLeaderRepository groupLeaderRepository;
    private final GroupCurriculumOverrideRepository overrideRepository;
    private final ProgramApi programApi;
    private final StudentApi studentApi;
    private final TeacherApi teacherApi;

    @Override
    public Optional<StudentGroupDto> findGroupById(UUID id) {
        return studentGroupRepository.findById(id).map(this::toGroupDto);
    }

    @Override
    public Optional<StudentGroupDto> findGroupByCode(String code) {
        return studentGroupRepository.findByCode(code).map(this::toGroupDto);
    }

    @Override
    public List<StudentGroupDto> findAllGroups() {
        return studentGroupRepository.findAll().stream()
                .map(this::toGroupDto)
                .toList();
    }

    @Override
    public List<StudentGroupDto> findGroupsByProgramId(UUID programId) {
        return studentGroupRepository.findByProgramId(programId).stream()
                .map(this::toGroupDto)
                .toList();
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
            UUID curatorTeacherId
    ) {
        if (programApi.findProgramById(programId).isEmpty()) {
            throw new IllegalArgumentException("Program not found: " + programId);
        }
        if (programApi.findCurriculumById(curriculumId).isEmpty()) {
            throw new IllegalArgumentException("Curriculum not found: " + curriculumId);
        }
        if (curatorTeacherId != null && teacherApi.findById(curatorTeacherId).isEmpty()) {
            throw new IllegalArgumentException("Teacher not found: " + curatorTeacherId);
        }
        if (studentGroupRepository.existsByCode(code)) {
            throw new IllegalArgumentException("Group with code " + code + " already exists");
        }
        StudentGroup entity = StudentGroup.builder()
                .programId(programId)
                .curriculumId(curriculumId)
                .code(code)
                .name(name)
                .description(description)
                .startYear(startYear)
                .graduationYear(graduationYear)
                .curatorTeacherId(curatorTeacherId)
                .build();
        return toGroupDto(studentGroupRepository.save(entity));
    }

    @Override
    @Transactional
    public StudentGroupDto updateGroup(
            UUID id,
            String name,
            String description,
            Integer graduationYear,
            UUID curatorTeacherId
    ) {
        StudentGroup entity = studentGroupRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Group not found: " + id));
        if (curatorTeacherId != null && teacherApi.findById(curatorTeacherId).isEmpty()) {
            throw new IllegalArgumentException("Teacher not found: " + curatorTeacherId);
        }
        if (name != null) entity.setName(name);
        if (description != null) entity.setDescription(description);
        if (graduationYear != null) entity.setGraduationYear(graduationYear);
        entity.setCuratorTeacherId(curatorTeacherId);
        entity.setUpdatedAt(LocalDateTime.now());
        return toGroupDto(studentGroupRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteGroup(UUID id) {
        if (!studentGroupRepository.existsById(id)) {
            throw new IllegalArgumentException("Group not found: " + id);
        }
        studentGroupRepository.deleteById(id);
    }

    @Override
    public List<GroupLeaderDto> findLeadersByGroupId(UUID groupId) {
        return groupLeaderRepository.findByGroupId(groupId).stream()
                .map(this::toLeaderDto)
                .toList();
    }

    @Override
    @Transactional
    public GroupLeaderDto addGroupLeader(UUID groupId, UUID studentId, String role, LocalDate fromDate, LocalDate toDate) {
        if (studentGroupRepository.findById(groupId).isEmpty()) {
            throw new IllegalArgumentException("Group not found: " + groupId);
        }
        if (studentApi.findById(studentId).isEmpty()) {
            throw new IllegalArgumentException("Student not found: " + studentId);
        }
        if (!"headman".equals(role) && !"deputy".equals(role)) {
            throw new IllegalArgumentException("Role must be headman or deputy");
        }
        if (groupLeaderRepository.existsByGroupIdAndStudentIdAndRole(groupId, studentId, role)) {
            throw new IllegalArgumentException("Leader with this role already exists for group/student");
        }
        GroupLeader entity = GroupLeader.builder()
                .groupId(groupId)
                .studentId(studentId)
                .role(role)
                .fromDate(fromDate)
                .toDate(toDate)
                .build();
        return toLeaderDto(groupLeaderRepository.save(entity));
    }

    @Override
    @Transactional
    public void removeGroupLeader(UUID id) {
        if (!groupLeaderRepository.existsById(id)) {
            throw new IllegalArgumentException("Group leader not found: " + id);
        }
        groupLeaderRepository.deleteById(id);
    }

    @Override
    public List<GroupCurriculumOverrideDto> findOverridesByGroupId(UUID groupId) {
        return overrideRepository.findByGroupId(groupId).stream()
                .map(this::toOverrideDto)
                .toList();
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
        if (studentGroupRepository.findById(groupId).isEmpty()) {
            throw new IllegalArgumentException("Group not found: " + groupId);
        }
        if (!"ADD".equals(action) && !"REMOVE".equals(action) && !"REPLACE".equals(action)) {
            throw new IllegalArgumentException("Action must be ADD, REMOVE, or REPLACE");
        }
        if ("REMOVE".equals(action) && curriculumSubjectId == null) {
            throw new IllegalArgumentException("curriculumSubjectId required for REMOVE");
        }
        if ("ADD".equals(action) && subjectId == null) {
            throw new IllegalArgumentException("subjectId required for ADD");
        }
        if ("REPLACE".equals(action) && curriculumSubjectId == null) {
            throw new IllegalArgumentException("curriculumSubjectId required for REPLACE");
        }
        GroupCurriculumOverride entity = GroupCurriculumOverride.builder()
                .groupId(groupId)
                .curriculumSubjectId(curriculumSubjectId)
                .subjectId(subjectId)
                .action(action)
                .newAssessmentTypeId(newAssessmentTypeId)
                .newDurationWeeks(newDurationWeeks)
                .reason(reason)
                .build();
        return toOverrideDto(overrideRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteOverride(UUID id) {
        if (!overrideRepository.existsById(id)) {
            throw new IllegalArgumentException("Override not found: " + id);
        }
        overrideRepository.deleteById(id);
    }

    private StudentGroupDto toGroupDto(StudentGroup e) {
        return new StudentGroupDto(e.getId(), e.getProgramId(), e.getCurriculumId(), e.getCode(), e.getName(),
                e.getDescription(), e.getStartYear(), e.getGraduationYear(), e.getCuratorTeacherId(),
                e.getCreatedAt(), e.getUpdatedAt());
    }

    private GroupLeaderDto toLeaderDto(GroupLeader e) {
        return new GroupLeaderDto(e.getId(), e.getGroupId(), e.getStudentId(), e.getRole(),
                e.getFromDate(), e.getToDate(), e.getCreatedAt());
    }

    private GroupCurriculumOverrideDto toOverrideDto(GroupCurriculumOverride e) {
        return new GroupCurriculumOverrideDto(e.getId(), e.getGroupId(), e.getCurriculumSubjectId(), e.getSubjectId(),
                e.getAction(), e.getNewAssessmentTypeId(), e.getNewDurationWeeks(), e.getReason(), e.getCreatedAt());
    }
}
