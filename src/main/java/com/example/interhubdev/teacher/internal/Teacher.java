package com.example.interhubdev.teacher.internal;

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
 * Teacher profile entity.
 * Linked to User entity via userId (OneToOne relationship).
 * Contains professional information specific to teachers.
 * 
 * <p>Package-private: only accessible within the teacher module.</p>
 */
@Entity
@Table(name = "teachers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Reference to the User entity.
     * Each teacher profile belongs to exactly one user with TEACHER role.
     */
    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    /**
     * Unique teacher ID (personnel number) assigned by the university.
     */
    @Column(name = "teacher_id", nullable = false, unique = true, length = 50)
    private String teacherId;

    /**
     * Faculty/department the teacher belongs to.
     */
    @Column(name = "faculty", nullable = false, length = 100)
    private String faculty;

    /**
     * Teacher's English name (for international communication).
     */
    @Column(name = "english_name", length = 100)
    private String englishName;

    /**
     * Academic position (e.g., Professor, Associate Professor, Lecturer).
     */
    @Column(name = "position", length = 100)
    private String position;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
