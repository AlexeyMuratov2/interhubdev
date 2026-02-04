package com.example.interhubdev.group.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface GroupLeaderRepository extends JpaRepository<GroupLeader, UUID> {

    List<GroupLeader> findByGroupId(UUID groupId);

    /** Stable ordering for UI: role, then createdAt. */
    List<GroupLeader> findByGroupIdOrderByRoleAscCreatedAtAsc(UUID groupId);

    boolean existsByGroupIdAndStudentIdAndRole(UUID groupId, UUID studentId, String role);
}
