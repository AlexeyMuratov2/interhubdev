package com.example.interhubdev.group;

import com.example.interhubdev.error.AppException;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public API for Group module: student groups, group leaders, curriculum overrides, and group membership.
 * All errors are thrown as {@link AppException} (via {@link com.example.interhubdev.error.Errors} or
 * module's GroupErrors) and handled by global exception handler.
 */
public interface GroupApi {

    // --- Student group ---

    /**
     * Find group by id.
     *
     * @param id group id
     * @return optional group DTO if found
     */
    Optional<StudentGroupDto> findGroupById(UUID id);

    /**
     * Find groups by ids (batch). Missing ids are skipped; order is not guaranteed.
     *
     * @param ids group ids (must not be null)
     * @return list of group DTOs found (never null)
     */
    List<StudentGroupDto> findGroupsByIds(Collection<UUID> ids);

    /**
     * Find group by code.
     *
     * @param code group code
     * @return optional group DTO if found
     */
    Optional<StudentGroupDto> findGroupByCode(String code);

    /**
     * List all groups ordered by program and code.
     *
     * @return list of group DTOs
     */
    List<StudentGroupDto> findAllGroups();

    /**
     * List groups by program id.
     *
     * @param programId program id
     * @return list of group DTOs
     */
    List<StudentGroupDto> findGroupsByProgramId(UUID programId);

    /**
     * Create a new student group.
     *
     * @param programId      program id (required)
     * @param curriculumId   curriculum id (required)
     * @param code           group code (required, unique)
     * @param name           optional name
     * @param description    optional description
     * @param startYear      start year (valid range per module)
     * @param graduationYear optional graduation year
     * @param curatorUserId  optional curator user id
     * @return created group DTO
     * @throws AppException e.g. not found (program/curriculum/user), conflict (code exists), bad request (validation)
     */
    StudentGroupDto createGroup(
            UUID programId,
            UUID curriculumId,
            String code,
            String name,
            String description,
            int startYear,
            Integer graduationYear,
            UUID curatorUserId
    );

    /**
     * Update group by id.
     *
     * @param id             group id
     * @param name           optional name
     * @param description    optional description
     * @param graduationYear optional graduation year
     * @param curatorUserId  optional curator user id
     * @return updated group DTO
     * @throws AppException e.g. not found (group/user), bad request
     */
    StudentGroupDto updateGroup(
            UUID id,
            String name,
            String description,
            Integer graduationYear,
            UUID curatorUserId
    );

    /**
     * Delete group by id.
     *
     * @param id group id
     * @throws AppException e.g. not found
     */
    void deleteGroup(UUID id);

    /**
     * Add one student to group. Idempotent if already a member.
     *
     * @param groupId   group id
     * @param studentId student id
     * @throws AppException e.g. group not found, student not found
     */
    void addGroupMember(UUID groupId, UUID studentId);

    /**
     * Add multiple students to group. Idempotent per student. Empty list is no-op.
     *
     * @param groupId    group id
     * @param studentIds list of student ids
     * @throws AppException e.g. group not found
     */
    void addGroupMembersBulk(UUID groupId, List<UUID> studentIds);

    /**
     * Remove student from group.
     *
     * @param groupId   group id
     * @param studentId student id
     * @throws AppException e.g. group not found
     */
    void removeGroupMember(UUID groupId, UUID studentId);

    /**
     * Get group members with full student and user data (for display names).
     *
     * @param groupId group id
     * @return list of member DTOs with user info
     * @throws AppException e.g. group not found
     */
    List<GroupMemberDto> getGroupMembersWithUsers(UUID groupId);

    // --- Group leader ---

    /**
     * Get group leaders with full student and user data (for display names).
     *
     * @param groupId group id
     * @return list of leader detail DTOs
     */
    List<GroupLeaderDetailDto> findLeadersByGroupId(UUID groupId);

    /**
     * Add group leader (headman or deputy).
     *
     * @param groupId   group id
     * @param studentId student id
     * @param role      role (headman or deputy)
     * @param fromDate  optional from date
     * @param toDate    optional to date
     * @return created leader DTO
     * @throws AppException e.g. not found (group/student), conflict (role already exists), bad request (role validation)
     */
    GroupLeaderDto addGroupLeader(UUID groupId, UUID studentId, String role, LocalDate fromDate, LocalDate toDate);

    /**
     * Remove group leader by id.
     *
     * @param id group leader id
     * @throws AppException e.g. not found
     */
    void removeGroupLeader(UUID id);

    // --- Group curriculum override ---

    /**
     * List curriculum overrides for group.
     *
     * @param groupId group id
     * @return list of override DTOs
     */
    List<GroupCurriculumOverrideDto> findOverridesByGroupId(UUID groupId);

    /**
     * Create curriculum override for group.
     *
     * @param groupId               group id
     * @param curriculumSubjectId  curriculum subject id (required for REMOVE/REPLACE)
     * @param subjectId             subject id (required for ADD)
     * @param action                ADD, REMOVE, or REPLACE
     * @param newAssessmentTypeId   optional (for ADD/REPLACE)
     * @param newDurationWeeks     optional (for ADD/REPLACE)
     * @param reason                optional reason
     * @return created override DTO
     * @throws AppException e.g. not found (group), bad request (action/ids validation)
     */
    GroupCurriculumOverrideDto createOverride(
            UUID groupId,
            UUID curriculumSubjectId,
            UUID subjectId,
            String action,
            UUID newAssessmentTypeId,
            Integer newDurationWeeks,
            String reason
    );

    /**
     * Delete curriculum override by id.
     *
     * @param id override id
     * @throws AppException e.g. not found
     */
    void deleteOverride(UUID id);
}
