package com.example.interhubdev.program.internal;

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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "curriculum_subject_assessment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class CurriculumSubjectAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "curriculum_subject_id", nullable = false)
    private UUID curriculumSubjectId;

    @Column(name = "assessment_type_id", nullable = false)
    private UUID assessmentTypeId;

    @Column(name = "week_number")
    private Integer weekNumber;

    @Column(name = "is_final", nullable = false)
    @Builder.Default
    private boolean isFinal = false;

    @Column(name = "weight", precision = 3, scale = 2)
    private BigDecimal weight;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
