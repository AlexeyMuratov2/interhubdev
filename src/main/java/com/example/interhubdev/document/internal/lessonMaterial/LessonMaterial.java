package com.example.interhubdev.document.internal.lessonMaterial;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA entity for lesson material (business entity linking a lesson to stored files).
 * One lesson has many materials; one material has many files via LessonMaterialFile.
 */
@Entity
@Table(name = "lesson_material")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class LessonMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "lesson_id", nullable = false)
    private UUID lessonId;

    @Column(name = "name", nullable = false, length = 500)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @OneToMany(mappedBy = "lessonMaterial", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<LessonMaterialFile> files = new ArrayList<>();
}
