package com.example.interhubdev.schedule.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface BuildingRepository extends JpaRepository<Building, UUID> {

    List<Building> findAllByOrderByNameAsc();
}
