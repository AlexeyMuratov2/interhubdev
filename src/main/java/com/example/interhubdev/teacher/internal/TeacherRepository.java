package com.example.interhubdev.teacher.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Teacher entity.
 * Package-private: only accessible within the teacher module.
 */
interface TeacherRepository extends JpaRepository<Teacher, UUID> {

    Optional<Teacher> findByUserId(UUID userId);

    Optional<Teacher> findByTeacherId(String teacherId);

    boolean existsByTeacherId(String teacherId);

    boolean existsByUserId(UUID userId);

    List<Teacher> findByFaculty(String faculty);

    List<Teacher> findFirst31ByOrderByIdAsc();

    List<Teacher> findFirst31ByIdGreaterThanOrderByIdAsc(UUID after);
}
