package com.example.interhubdev.teacher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public API for the Teacher module.
 * Manages teacher profiles linked to users with TEACHER role.
 */
public interface TeacherApi {

    /**
     * Find teacher profile by user ID.
     *
     * @param userId the user ID
     * @return teacher profile if exists
     */
    Optional<TeacherDto> findByUserId(UUID userId);

    /**
     * Find teacher profile by teacher ID (personnel number).
     *
     * @param teacherId the teacher personnel ID
     * @return teacher profile if exists
     */
    Optional<TeacherDto> findByTeacherId(String teacherId);

    /**
     * Check if teacher with given teacher ID exists.
     *
     * @param teacherId the teacher personnel ID
     * @return true if exists
     */
    boolean existsByTeacherId(String teacherId);

    /**
     * Check if user already has a teacher profile.
     *
     * @param userId the user ID
     * @return true if profile exists
     */
    boolean existsByUserId(UUID userId);

    /**
     * Find teacher profile by ID.
     *
     * @param id the teacher entity ID
     * @return teacher profile if exists
     */
    Optional<TeacherDto> findById(UUID id);

    /**
     * Find teachers by ids (batch). Missing ids are skipped; order is not guaranteed.
     *
     * @param ids teacher entity ids (must not be null)
     * @return list of teacher DTOs found (never null)
     */
    List<TeacherDto> findByIds(List<UUID> ids);

    /**
     * Get all teachers.
     *
     * @return list of all teacher profiles
     */
    List<TeacherDto> findAll();

    /**
     * List teachers with cursor-based pagination. Ordered by id ascending.
     *
     * @param cursor optional cursor (last teacher entity id from previous page); null for first page
     * @param limit  max items per page (will be capped at 30)
     * @return page with items and optional next cursor
     */
    TeacherPage listTeachers(UUID cursor, int limit);

    /**
     * Find teachers by faculty.
     *
     * @param faculty faculty name
     * @return list of teachers in the faculty
     */
    List<TeacherDto> findByFaculty(String faculty);

    /**
     * Create a teacher profile for a user.
     *
     * @param userId  the user ID (must have TEACHER role)
     * @param request teacher profile data
     * @return created teacher profile
     * @throws IllegalArgumentException if user not found, not a teacher, or teacherId already exists
     * @throws IllegalStateException    if user already has a teacher profile
     */
    TeacherDto create(UUID userId, CreateTeacherRequest request);

    /**
     * Update teacher profile.
     *
     * @param userId  the user ID
     * @param request updated data (null fields are ignored)
     * @return updated teacher profile
     * @throws IllegalArgumentException if profile not found
     */
    TeacherDto update(UUID userId, CreateTeacherRequest request);

    /**
     * Delete teacher profile.
     *
     * @param userId the user ID
     * @throws IllegalArgumentException if profile not found
     */
    void delete(UUID userId);
}
