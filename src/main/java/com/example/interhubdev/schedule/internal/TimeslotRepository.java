package com.example.interhubdev.schedule.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface TimeslotRepository extends JpaRepository<Timeslot, UUID> {
}
