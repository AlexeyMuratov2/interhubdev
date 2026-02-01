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
