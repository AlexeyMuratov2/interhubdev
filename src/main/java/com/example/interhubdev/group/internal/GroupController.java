package com.example.interhubdev.group.internal;

import com.example.interhubdev.group.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for groups, group members, leaders and curriculum overrides. Delegates to {@link GroupApi} only.
 */
@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@Tag(name = "Groups", description = "Student groups, leaders, curriculum overrides")
class GroupController {

    private final GroupApi groupApi;

    @GetMapping
    @Operation(summary = "Get all groups")
    public ResponseEntity<List<StudentGroupDto>> findAllGroups() {
        return ResponseEntity.ok(groupApi.findAllGroups());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get group by ID")
    public ResponseEntity<StudentGroupDto> findGroupById(@PathVariable UUID id) {
        return ResponseEntity.ok(groupApi.findGroupById(id)
                .orElseThrow(() -> GroupErrors.groupNotFound(id)));
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get group by code")
    public ResponseEntity<StudentGroupDto> findGroupByCode(@PathVariable String code) {
        return ResponseEntity.ok(groupApi.findGroupByCode(code)
                .orElseThrow(() -> GroupErrors.groupNotFoundByCode(code)));
    }

    @GetMapping("/program/{programId}")
    @Operation(summary = "Get groups by program ID")
    public ResponseEntity<List<StudentGroupDto>> findGroupsByProgramId(@PathVariable UUID programId) {
        return ResponseEntity.ok(groupApi.findGroupsByProgramId(programId));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create group", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can create groups")
    public ResponseEntity<StudentGroupDto> createGroup(@Valid @RequestBody CreateGroupRequest request) {
        StudentGroupDto dto = groupApi.createGroup(
                request.programId(),
                request.curriculumId(),
                request.code(),
                request.name(),
                request.description(),
                request.startYear(),
                request.graduationYear(),
                request.curatorUserId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update group", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can update groups")
    public ResponseEntity<StudentGroupDto> updateGroup(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateGroupRequest request
    ) {
        StudentGroupDto dto = groupApi.updateGroup(
                id,
                request.name(),
                request.description(),
                request.graduationYear(),
                request.curatorUserId()
        );
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete group", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can delete groups")
    public ResponseEntity<Void> deleteGroup(@PathVariable UUID id) {
        groupApi.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{groupId}/members")
    @Operation(summary = "Get group members", description = "List students belonging to the group (n:m) with user data for display names")
    public ResponseEntity<List<GroupMemberDto>> getGroupMembers(@PathVariable UUID groupId) {
        return ResponseEntity.ok(groupApi.getGroupMembersWithUsers(groupId));
    }

    @PostMapping("/{groupId}/members")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Add student to group", description = "Add one student. Idempotent if already a member.")
    public ResponseEntity<Void> addGroupMember(
            @PathVariable UUID groupId,
            @Valid @RequestBody AddGroupMemberRequest request
    ) {
        groupApi.addGroupMember(groupId, request.studentId());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/{groupId}/members/bulk")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Add multiple students to group", description = "Add many students at once. Idempotent per student. Empty list is no-op.")
    public ResponseEntity<Void> addGroupMembersBulk(
            @PathVariable UUID groupId,
            @Valid @RequestBody AddGroupMembersBulkRequest request
    ) {
        groupApi.addGroupMembersBulk(groupId, request.studentIds());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/{groupId}/members/{studentId}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Remove student from group")
    public ResponseEntity<Void> removeGroupMember(
            @PathVariable UUID groupId,
            @PathVariable UUID studentId
    ) {
        groupApi.removeGroupMember(groupId, studentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{groupId}/leaders")
    @Operation(summary = "Get group leaders", description = "Returns leaders with full student and user data for display names")
    public ResponseEntity<List<GroupLeaderDetailDto>> findLeadersByGroupId(@PathVariable UUID groupId) {
        return ResponseEntity.ok(groupApi.findLeadersByGroupId(groupId));
    }

    @PostMapping("/{groupId}/leaders")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Add group leader", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can add group leaders")
    public ResponseEntity<GroupLeaderDto> addGroupLeader(
            @PathVariable UUID groupId,
            @Valid @RequestBody AddGroupLeaderRequest request
    ) {
        GroupLeaderDto dto = groupApi.addGroupLeader(
                groupId,
                request.studentId(),
                request.role(),
                request.fromDate(),
                request.toDate()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @DeleteMapping("/leaders/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Remove group leader", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can remove group leaders")
    public ResponseEntity<Void> removeGroupLeader(@PathVariable UUID id) {
        groupApi.removeGroupLeader(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{groupId}/overrides")
    @Operation(summary = "Get curriculum overrides for group")
    public ResponseEntity<List<GroupCurriculumOverrideDto>> findOverridesByGroupId(@PathVariable UUID groupId) {
        return ResponseEntity.ok(groupApi.findOverridesByGroupId(groupId));
    }

    @PostMapping("/{groupId}/overrides")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create curriculum override", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can create overrides")
    public ResponseEntity<GroupCurriculumOverrideDto> createOverride(
            @PathVariable UUID groupId,
            @Valid @RequestBody CreateOverrideRequest request
    ) {
        GroupCurriculumOverrideDto dto = groupApi.createOverride(
                groupId,
                request.curriculumSubjectId(),
                request.subjectId(),
                request.action(),
                request.newAssessmentTypeId(),
                request.newDurationWeeks(),
                request.reason()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @DeleteMapping("/overrides/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete curriculum override", description = "Only MODERATOR, ADMIN, SUPER_ADMIN can delete overrides")
    public ResponseEntity<Void> deleteOverride(@PathVariable UUID id) {
        groupApi.deleteOverride(id);
        return ResponseEntity.noContent().build();
    }

    record AddGroupMemberRequest(@NotNull(message = "Student id is required") UUID studentId) {}

    record AddGroupMembersBulkRequest(@NotNull(message = "Student ids are required") List<UUID> studentIds) {}

    record CreateGroupRequest(
            @NotNull(message = "Program id is required") UUID programId,
            @NotNull(message = "Curriculum id is required") UUID curriculumId,
            @NotBlank(message = "Code is required") String code,
            String name,
            String description,
            @Min(value = 1900, message = "startYear must be at least 1900") @Max(value = 2100, message = "startYear must be at most 2100") int startYear,
            Integer graduationYear,
            UUID curatorUserId
    ) {}
    record UpdateGroupRequest(String name, String description, Integer graduationYear, UUID curatorUserId) {}
    record AddGroupLeaderRequest(
            @NotNull(message = "Student id is required") UUID studentId,
            @NotBlank(message = "Role is required") String role,
            LocalDate fromDate,
            LocalDate toDate
    ) {}
    record CreateOverrideRequest(UUID curriculumSubjectId, UUID subjectId,
                                @NotBlank(message = "Action is required") String action,
                                UUID newAssessmentTypeId, Integer newDurationWeeks, String reason) {}
}
