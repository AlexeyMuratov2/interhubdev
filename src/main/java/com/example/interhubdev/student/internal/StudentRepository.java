package com.example.interhubdev.student.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Student entity.
 * Package-private: only accessible within the student module.
 */
interface StudentRepository extends JpaRepository<Student, UUID> {

    Optional<Student> findByUserId(UUID userId);

    Optional<Student> findByStudentId(String studentId);

    boolean existsByStudentId(String studentId);

    boolean existsByUserId(UUID userId);

    List<Student> findByFaculty(String faculty);

    List<Student> findByGroupName(String groupName);
}
