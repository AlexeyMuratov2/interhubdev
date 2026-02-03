package com.example.interhubdev.group.internal;

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

@Entity
@Table(name = "group_curriculum_override")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class GroupCurriculumOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "curriculum_subject_id")
    private UUID curriculumSubjectId;

    @Column(name = "subject_id")
    private UUID subjectId;

    @Column(name = "action", nullable = false, columnDefinition = "group_curriculum_override_action")
    private String action;

    @Column(name = "new_assessment_type_id")
    private UUID newAssessmentTypeId;

    @Column(name = "new_duration_weeks")
    private Integer newDurationWeeks;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
