package com.example.interhubdev.subject.internal;

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
 * JPA entity for an assessment type (e.g. exam, test, coursework).
 * Code is unique. sortOrder determines display order in lists/dropdowns.
 */
@Entity
@Table(name = "assessment_type")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class AssessmentType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "chinese_name", nullable = false, length = 255)
    private String chineseName;

    @Column(name = "english_name", length = 255)
    private String englishName;

    @Column(name = "is_graded", nullable = false)
    @Builder.Default
    private Boolean isGraded = true;

    @Column(name = "is_final", nullable = false)
    @Builder.Default
    private Boolean isFinal = false;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
