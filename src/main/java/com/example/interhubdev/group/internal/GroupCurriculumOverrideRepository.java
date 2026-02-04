package com.example.interhubdev.group.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface GroupCurriculumOverrideRepository extends JpaRepository<GroupCurriculumOverride, UUID> {

    List<GroupCurriculumOverride> findByGroupId(UUID groupId);

    /** Stable ordering for UI: newest first. */
    List<GroupCurriculumOverride> findByGroupIdOrderByCreatedAtDesc(UUID groupId);
}
