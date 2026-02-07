package com.example.interhubdev.schedule.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
import java.util.UUID;

interface RoomRepository extends JpaRepository<Room, UUID> {

    List<Room> findAllByOrderByBuilding_NameAscNumberAsc();

    @Query("SELECT r FROM Room r JOIN FETCH r.building WHERE r.id IN :ids")
    List<Room> findAllByIdInWithBuilding(@Param("ids") Set<UUID> ids);

    long countByBuildingId(UUID buildingId);
}
