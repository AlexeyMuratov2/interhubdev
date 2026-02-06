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
import java.time.LocalTime;
import java.util.UUID;

/**
 * Weekly recurring slot for an offering. Owns day of week and time (start_time, end_time).
 * timeslot_id is optional (UI hint when created from a timeslot template).
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

    @Column(name = "day_of_week", nullable = false)
    private int dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "timeslot_id")
    private UUID timeslotId;

    @Column(name = "lesson_type", nullable = false, length = 50)
    private String lessonType;

    @Column(name = "room_id")
    private UUID roomId;

    @Column(name = "teacher_id")
    private UUID teacherId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
