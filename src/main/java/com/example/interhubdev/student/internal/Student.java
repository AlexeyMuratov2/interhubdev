package com.example.interhubdev.student.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Student profile entity.
 * Linked to User entity via userId (OneToOne relationship).
 * Contains academic information specific to students.
 * 
 * <p>Package-private: only accessible within the student module.</p>
 */
@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Reference to the User entity.
     * Each student profile belongs to exactly one user with STUDENT role.
     */
    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    /**
     * Unique student ID assigned by the university.
     */
    @Column(name = "student_id", nullable = false, unique = true, length = 50)
    private String studentId;

    /**
     * Student's Chinese name (for international students from China).
     */
    @Column(name = "chinese_name", length = 100)
    private String chineseName;

    /**
     * Faculty/department the student belongs to.
     */
    @Column(name = "faculty", nullable = false, length = 100)
    private String faculty;

    /**
     * Course/program of study.
     */
    @Column(name = "course", length = 100)
    private String course;

    /**
     * Year when the student enrolled.
     */
    @Column(name = "enrollment_year")
    private Integer enrollmentYear;

    /**
     * Student's academic group.
     */
    @Column(name = "group_name", length = 50)
    private String groupName;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
