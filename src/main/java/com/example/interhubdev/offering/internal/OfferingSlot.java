package com.example.interhubdev.offering.internal;

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
 * Weekly recurring timeslot for an offering.
 * Defines when (day of week + time) a particular lesson type occurs for an offering.
 */
@Entity
@Table(name = "offering_slot")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class OfferingSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "offering_id", nullable = false)
    private UUID offeringId;

    @Column(name = "timeslot_id", nullable = false)
    private UUID timeslotId;

    /** Lesson type: LECTURE, PRACTICE, LAB, SEMINAR. */
    @Column(name = "lesson_type", nullable = false, length = 50)
    private String lessonType;

    /** Optional room override (if different from offering's default room). */
    @Column(name = "room_id")
    private UUID roomId;

    /** Optional teacher override (if different from offering's default teacher). */
    @Column(name = "teacher_id")
    private UUID teacherId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
