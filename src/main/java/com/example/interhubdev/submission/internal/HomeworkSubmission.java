package com.example.interhubdev.submission.internal;

import jakarta.persistence.*;
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
 * JPA entity for a student's homework submission.
 * Files are optional (stored in HomeworkSubmissionFile).
 */
@Entity
@Table(name = "homework_submission")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class HomeworkSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "homework_id", nullable = false)
    private UUID homeworkId;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime submittedAt = LocalDateTime.now();

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @OneToMany(mappedBy = "submission", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<HomeworkSubmissionFile> files = new ArrayList<>();
}
