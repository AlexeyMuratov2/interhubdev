package com.example.interhubdev.group.internal;

import com.example.interhubdev.group.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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
        return groupApi.findGroupById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get group by code")
    public ResponseEntity<StudentGroupDto> findGroupByCode(@PathVariable String code) {
        return groupApi.findGroupByCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/program/{programId}")
    @Operation(summary = "Get groups by program ID")
    public ResponseEntity<List<StudentGroupDto>> findGroupsByProgramId(@PathVariable UUID programId) {
        return ResponseEntity.ok(groupApi.findGroupsByProgramId(programId));
    }

    @PostMapping
    @Operation(summary = "Create group")
    public ResponseEntity<StudentGroupDto> createGroup(@RequestBody CreateGroupRequest request) {
        StudentGroupDto dto = groupApi.createGroup(
                request.programId(),
                request.curriculumId(),
                request.code(),
                request.name(),
                request.description(),
                request.startYear(),
                request.graduationYear(),
                request.curatorTeacherId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update group")
    public ResponseEntity<StudentGroupDto> updateGroup(
            @PathVariable UUID id,
            @RequestBody UpdateGroupRequest request
    ) {
        StudentGroupDto dto = groupApi.updateGroup(
                id,
                request.name(),
                request.description(),
                request.graduationYear(),
                request.curatorTeacherId()
        );
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete group")
    public ResponseEntity<Void> deleteGroup(@PathVariable UUID id) {
        groupApi.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{groupId}/leaders")
    @Operation(summary = "Get group leaders")
    public ResponseEntity<List<GroupLeaderDto>> findLeadersByGroupId(@PathVariable UUID groupId) {
        return ResponseEntity.ok(groupApi.findLeadersByGroupId(groupId));
    }

    @PostMapping("/{groupId}/leaders")
    @Operation(summary = "Add group leader")
    public ResponseEntity<GroupLeaderDto> addGroupLeader(
            @PathVariable UUID groupId,
            @RequestBody AddGroupLeaderRequest request
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
    @Operation(summary = "Remove group leader")
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
    @Operation(summary = "Create curriculum override")
    public ResponseEntity<GroupCurriculumOverrideDto> createOverride(
            @PathVariable UUID groupId,
            @RequestBody CreateOverrideRequest request
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
    @Operation(summary = "Delete curriculum override")
    public ResponseEntity<Void> deleteOverride(@PathVariable UUID id) {
        groupApi.deleteOverride(id);
        return ResponseEntity.noContent().build();
    }

    record CreateGroupRequest(UUID programId, UUID curriculumId, String code, String name, String description,
                             int startYear, Integer graduationYear, UUID curatorTeacherId) {}
    record UpdateGroupRequest(String name, String description, Integer graduationYear, UUID curatorTeacherId) {}
    record AddGroupLeaderRequest(UUID studentId, String role, LocalDate fromDate, LocalDate toDate) {}
    record CreateOverrideRequest(UUID curriculumSubjectId, UUID subjectId, String action,
                                UUID newAssessmentTypeId, Integer newDurationWeeks, String reason) {}
}
