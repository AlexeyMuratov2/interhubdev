package com.example.interhubdev.document.internal.homework;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.UUID;

/**
 * JPA entity for junction table linking lessons to homework assignments.
 * Each homework is linked to exactly one lesson (enforced by unique constraint on homeworkId).
 */
@Entity
@Table(name = "lesson_homework")
@IdClass(LessonHomework.LessonHomeworkId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
class LessonHomework {

    @Id
    @Column(name = "lesson_id", nullable = false)
    private UUID lessonId;

    @Id
    @Column(name = "homework_id", nullable = false, unique = true)
    private UUID homeworkId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homework_id", insertable = false, updatable = false)
    private Homework homework;

    /**
     * Composite key for LessonHomework entity.
     */
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    static class LessonHomeworkId implements Serializable {
        private UUID lessonId;
        private UUID homeworkId;
    }
}
