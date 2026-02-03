package com.example.interhubdev.group.internal;

import com.example.interhubdev.error.Errors;
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

    private static final int MIN_YEAR = 1900;
    private static final int MAX_YEAR = 2100;

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
        if (programId == null) {
            throw Errors.badRequest("Program id is required");
        }
        if (curriculumId == null) {
            throw Errors.badRequest("Curriculum id is required");
        }
        if (code == null || code.isBlank()) {
            throw Errors.badRequest("Group code is required");
        }
        if (startYear < MIN_YEAR || startYear > MAX_YEAR) {
            throw Errors.badRequest("startYear must be between " + MIN_YEAR + " and " + MAX_YEAR);
        }
        if (programApi.findProgramById(programId).isEmpty()) {
            throw Errors.notFound("Program not found: " + programId);
        }
        if (programApi.findCurriculumById(curriculumId).isEmpty()) {
            throw Errors.notFound("Curriculum not found: " + curriculumId);
        }
        if (curatorTeacherId != null && teacherApi.findById(curatorTeacherId).isEmpty()) {
            throw Errors.notFound("Teacher not found: " + curatorTeacherId);
        }
        String trimmedCode = code.trim();
        if (studentGroupRepository.existsByCode(trimmedCode)) {
            throw Errors.conflict("Group with code '" + trimmedCode + "' already exists");
        }
        StudentGroup entity = StudentGroup.builder()
                .programId(programId)
                .curriculumId(curriculumId)
                .code(trimmedCode)
                .name(name != null ? name.trim() : null)
                .description(description != null ? description.trim() : null)
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
                .orElseThrow(() -> Errors.notFound("Group not found: " + id));
        if (curatorTeacherId != null && teacherApi.findById(curatorTeacherId).isEmpty()) {
            throw Errors.notFound("Teacher not found: " + curatorTeacherId);
        }
        if (name != null) entity.setName(name.trim());
        if (description != null) entity.setDescription(description.trim());
        if (graduationYear != null) entity.setGraduationYear(graduationYear);
        entity.setCuratorTeacherId(curatorTeacherId);
        entity.setUpdatedAt(LocalDateTime.now());
        return toGroupDto(studentGroupRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteGroup(UUID id) {
        if (!studentGroupRepository.existsById(id)) {
            throw Errors.notFound("Group not found: " + id);
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
        if (studentId == null) {
            throw Errors.badRequest("Student id is required");
        }
        if (role == null || role.isBlank()) {
            throw Errors.badRequest("Role is required");
        }
        if (studentGroupRepository.findById(groupId).isEmpty()) {
            throw Errors.notFound("Group not found: " + groupId);
        }
        if (studentApi.findById(studentId).isEmpty()) {
            throw Errors.notFound("Student not found: " + studentId);
        }
        String normalizedRole = role.trim().toLowerCase();
        if (!"headman".equals(normalizedRole) && !"deputy".equals(normalizedRole)) {
            throw Errors.badRequest("Role must be headman or deputy");
        }
        if (groupLeaderRepository.existsByGroupIdAndStudentIdAndRole(groupId, studentId, normalizedRole)) {
            throw Errors.conflict("Leader with this role already exists for group/student");
        }
        GroupLeader entity = GroupLeader.builder()
                .groupId(groupId)
                .studentId(studentId)
                .role(normalizedRole)
                .fromDate(fromDate)
                .toDate(toDate)
                .build();
        return toLeaderDto(groupLeaderRepository.save(entity));
    }

    @Override
    @Transactional
    public void removeGroupLeader(UUID id) {
        if (!groupLeaderRepository.existsById(id)) {
            throw Errors.notFound("Group leader not found: " + id);
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
        if (action == null || action.isBlank()) {
            throw Errors.badRequest("Action is required");
        }
        String normalizedAction = action.trim().toUpperCase();
        if (!"ADD".equals(normalizedAction) && !"REMOVE".equals(normalizedAction) && !"REPLACE".equals(normalizedAction)) {
            throw Errors.badRequest("Action must be ADD, REMOVE, or REPLACE");
        }
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
        return toOverrideDto(overrideRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteOverride(UUID id) {
        if (!overrideRepository.existsById(id)) {
            throw Errors.notFound("Override not found: " + id);
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
