package com.example.interhubdev.group.internal;

import com.example.interhubdev.error.Errors;
import com.example.interhubdev.group.GroupLeaderDetailDto;
import com.example.interhubdev.group.GroupLeaderDto;
import com.example.interhubdev.student.StudentApi;
import com.example.interhubdev.student.StudentDto;
import com.example.interhubdev.user.UserApi;
import com.example.interhubdev.user.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Internal service for group leaders (headman, deputy).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class GroupLeaderService {

    private final StudentGroupRepository studentGroupRepository;
    private final GroupLeaderRepository groupLeaderRepository;
    private final StudentApi studentApi;
    private final UserApi userApi;

    List<GroupLeaderDetailDto> findLeadersByGroupId(UUID groupId) {
        return groupLeaderRepository.findByGroupIdOrderByRoleAscCreatedAtAsc(groupId).stream()
                .map(leader -> {
                    GroupLeaderDto dto = GroupMappers.toLeaderDto(leader);
                    Optional<StudentDto> studentOpt = studentApi.findById(leader.getStudentId());
                    Optional<UserDto> userOpt = studentOpt.flatMap(s -> userApi.findById(s.userId()));
                    StudentDto student = studentOpt.orElse(null);
                    UserDto user = userOpt.orElse(null);
                    return new GroupLeaderDetailDto(
                            dto.id(), dto.groupId(), dto.studentId(), dto.role(),
                            dto.fromDate(), dto.toDate(), dto.createdAt(),
                            student, user
                    );
                })
                .toList();
    }

    @Transactional
    GroupLeaderDto addGroupLeader(UUID groupId, UUID studentId, String role, LocalDate fromDate, LocalDate toDate) {
        if (studentId == null) throw Errors.badRequest("Student id is required");
        String normalizedRole = GroupValidation.normalizeLeaderRole(role);

        if (studentGroupRepository.findById(groupId).isEmpty()) {
            throw GroupErrors.groupNotFound(groupId);
        }
        if (studentApi.findById(studentId).isEmpty()) {
            throw Errors.notFound("Student not found: " + studentId);
        }
        if (groupLeaderRepository.existsByGroupIdAndStudentIdAndRole(groupId, studentId, normalizedRole)) {
            throw GroupErrors.leaderRoleExists();
        }

        GroupLeader entity = GroupLeader.builder()
                .groupId(groupId)
                .studentId(studentId)
                .role(normalizedRole)
                .fromDate(fromDate)
                .toDate(toDate)
                .build();
        return GroupMappers.toLeaderDto(groupLeaderRepository.save(entity));
    }

    @Transactional
    void removeGroupLeader(UUID id) {
        if (!groupLeaderRepository.existsById(id)) {
            throw GroupErrors.groupLeaderNotFound(id);
        }
        groupLeaderRepository.deleteById(id);
    }

    /** Remove all leader records for given student in group (when member is removed from group). */
    @Transactional
    void removeLeadersForMember(UUID groupId, UUID studentId) {
        groupLeaderRepository.deleteByGroupIdAndStudentId(groupId, studentId);
    }
}

