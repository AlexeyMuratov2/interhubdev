package com.example.interhubdev.schedule.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface RoomRepository extends JpaRepository<Room, UUID> {
}
