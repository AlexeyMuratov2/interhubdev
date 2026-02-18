package com.example.interhubdev.document.internal.courseMaterial;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

import com.example.interhubdev.document.internal.storedFile.StoredFile;

/**
 * JPA entity for course material (business entity linking group_subject_offering to stored file).
 * CourseMaterial is separate from StoredFile to keep file storage generic.
 * Materials belong to a specific offering (group + curriculum_subject + teacher), allowing each teacher
 * to have their own materials for the same subject.
 */
@Entity
@Table(name = "course_material")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class CourseMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "offering_id", nullable = false)
    private UUID offeringId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stored_file_id", nullable = false)
    private StoredFile storedFile;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime uploadedAt = LocalDateTime.now();
}
