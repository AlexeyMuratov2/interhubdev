package com.example.interhubdev.group.internal;

import com.example.interhubdev.error.Errors;
import com.example.interhubdev.group.StudentGroupDto;
import com.example.interhubdev.program.ProgramApi;
import com.example.interhubdev.user.UserApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Internal service for group CRUD and catalog queries.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class GroupCatalogService {

    private final StudentGroupRepository studentGroupRepository;
    private final ProgramApi programApi;
    private final UserApi userApi;

    Optional<StudentGroupDto> findGroupById(UUID id) {
        return studentGroupRepository.findById(id).map(GroupMappers::toGroupDto);
    }

    Optional<StudentGroupDto> findGroupByCode(String code) {
        return studentGroupRepository.findByCode(code).map(GroupMappers::toGroupDto);
    }

    List<StudentGroupDto> findAllGroups() {
        return studentGroupRepository.findAllByOrderByProgramIdAscCodeAsc().stream()
                .map(GroupMappers::toGroupDto)
                .toList();
    }

    List<StudentGroupDto> findGroupsByProgramId(UUID programId) {
        return studentGroupRepository.findByProgramIdOrderByCodeAsc(programId).stream()
                .map(GroupMappers::toGroupDto)
                .toList();
    }

    @Transactional
    StudentGroupDto createGroup(
            UUID programId,
            UUID curriculumId,
            String code,
            String name,
            String description,
            int startYear,
            Integer graduationYear,
            UUID curatorUserId
    ) {
        if (programId == null) throw Errors.badRequest("Program id is required");
        if (curriculumId == null) throw Errors.badRequest("Curriculum id is required");

        String trimmedCode = GroupValidation.requiredTrimmed(code, "Group code");
        GroupValidation.validateYearRange(startYear, "startYear");

        if (programApi.findProgramById(programId).isEmpty()) {
            throw Errors.notFound("Program not found: " + programId);
        }
        if (programApi.findCurriculumById(curriculumId).isEmpty()) {
            throw Errors.notFound("Curriculum not found: " + curriculumId);
        }
        if (curatorUserId != null && userApi.findById(curatorUserId).isEmpty()) {
            throw Errors.notFound("User not found: " + curatorUserId);
        }
        if (studentGroupRepository.existsByCode(trimmedCode)) {
            throw GroupErrors.groupCodeExists(trimmedCode);
        }

        StudentGroup entity = StudentGroup.builder()
                .programId(programId)
                .curriculumId(curriculumId)
                .code(trimmedCode)
                .name(name != null ? name.trim() : null)
                .description(description != null ? description.trim() : null)
                .startYear(startYear)
                .graduationYear(graduationYear)
                .curatorUserId(curatorUserId)
                .build();
        return GroupMappers.toGroupDto(studentGroupRepository.save(entity));
    }

    @Transactional
    StudentGroupDto updateGroup(
            UUID id,
            String name,
            String description,
            Integer graduationYear,
            UUID curatorUserId
    ) {
        StudentGroup entity = studentGroupRepository.findById(id)
                .orElseThrow(() -> GroupErrors.groupNotFound(id));
        if (curatorUserId != null && userApi.findById(curatorUserId).isEmpty()) {
            throw Errors.notFound("User not found: " + curatorUserId);
        }
        if (name != null) entity.setName(name.trim());
        if (description != null) entity.setDescription(description.trim());
        if (graduationYear != null) entity.setGraduationYear(graduationYear);
        entity.setCuratorUserId(curatorUserId);
        entity.setUpdatedAt(LocalDateTime.now());
        return GroupMappers.toGroupDto(studentGroupRepository.save(entity));
    }

    @Transactional
    void deleteGroup(UUID id) {
        if (!studentGroupRepository.existsById(id)) {
            throw GroupErrors.groupNotFound(id);
        }
        studentGroupRepository.deleteById(id);
    }
}

