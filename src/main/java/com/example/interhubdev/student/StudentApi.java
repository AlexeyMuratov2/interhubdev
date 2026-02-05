package com.example.interhubdev.student;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


/**
 * Public API for the Student module.
 * Manages student profiles linked to users with STUDENT role.
 */
public interface StudentApi {

    /**
     * Find student profile by ID.
     *
     * @param id the student entity ID
     * @return student profile if exists
     */
    Optional<StudentDto> findById(UUID id);

    /**
     * Find student profile by user ID.
     *
     * @param userId the user ID
     * @return student profile if exists
     */
    Optional<StudentDto> findByUserId(UUID userId);

    /**
     * Find student profile by student ID (university ID).
     *
     * @param studentId the university student ID
     * @return student profile if exists
     */
    Optional<StudentDto> findByStudentId(String studentId);

    /**
     * Check if student with given student ID exists.
     *
     * @param studentId the university student ID
     * @return true if exists
     */
    boolean existsByStudentId(String studentId);

    /**
     * Check if user already has a student profile.
     *
     * @param userId the user ID
     * @return true if profile exists
     */
    boolean existsByUserId(UUID userId);

    /**
     * Get all students.
     *
     * @return list of all student profiles
     */
    List<StudentDto> findAll();

    /**
     * List students with cursor-based pagination. Ordered by id ascending.
     *
     * @param cursor optional cursor (last student entity id from previous page); null for first page
     * @param limit  max items per page (will be capped at 30)
     * @return page with items and optional next cursor
     */
    StudentPage listStudents(UUID cursor, int limit);

    /**
     * Find students by faculty.
     *
     * @param faculty faculty name
     * @return list of students in the faculty
     */
    List<StudentDto> findByFaculty(String faculty);

    /**
     * Find students by group.
     *
     * @param groupName group name
     * @return list of students in the group
     */
    List<StudentDto> findByGroupName(String groupName);

    /**
     * Find students by group ID (student_group.id). Uses n:m membership table.
     *
     * @param groupId student group UUID
     * @return list of students in the group
     */
    List<StudentDto> findByGroupId(UUID groupId);

    /**
     * Add student to a group. Idempotent if already a member.
     *
     * @param studentId student profile ID (students.id)
     * @param groupId   group ID
     * @throws IllegalArgumentException if student or group not found
     * @throws IllegalStateException    if already a member (optional, may be idempotent)
     */
    void addToGroup(UUID studentId, UUID groupId);

    /**
     * Add multiple students to a group. Idempotent per student (already members are skipped).
     *
     * @param groupId    group ID
     * @param studentIds list of student profile IDs (students.id); null or empty is no-op
     * @throws IllegalArgumentException if group not found or any student not found
     */
    void addToGroupBulk(UUID groupId, List<UUID> studentIds);

    /**
     * Remove student from a group.
     *
     * @param studentId student profile ID (students.id)
     * @param groupId   group ID
     */
    void removeFromGroup(UUID studentId, UUID groupId);

    /**
     * Get group IDs the student belongs to (by user ID).
     *
     * @param userId user ID of the student
     * @return list of group UUIDs; empty if no profile or no memberships
     */
    List<UUID> getGroupIdsByUserId(UUID userId);

    /**
     * Create a student profile for a user.
     *
     * @param userId  the user ID (must have STUDENT role)
     * @param request student profile data
     * @return created student profile
     * @throws IllegalArgumentException if user not found, not a student, or studentId already exists
     * @throws IllegalStateException    if user already has a student profile
     */
    StudentDto create(UUID userId, CreateStudentRequest request);

    /**
     * Update student profile.
     *
     * @param userId  the user ID
     * @param request updated data (null fields are ignored)
     * @return updated student profile
     * @throws IllegalArgumentException if profile not found
     */
    StudentDto update(UUID userId, CreateStudentRequest request);

    /**
     * Delete student profile.
     *
     * @param userId the user ID
     * @throws IllegalArgumentException if profile not found
     */
    void delete(UUID userId);
}
