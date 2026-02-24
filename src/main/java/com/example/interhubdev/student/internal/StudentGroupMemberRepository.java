package com.example.interhubdev.student.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface StudentGroupMemberRepository extends JpaRepository<StudentGroupMember, UUID> {

    List<StudentGroupMember> findByGroupId(UUID groupId);

    List<StudentGroupMember> findByStudentId(UUID studentId);

    boolean existsByStudentIdAndGroupId(UUID studentId, UUID groupId);

    void deleteByStudentIdAndGroupId(UUID studentId, UUID groupId);

    /**
     * Count members per group for the given group IDs. Returns (groupId, count) pairs.
     * Do not call with empty collection.
     */
    @Query("SELECT m.groupId, COUNT(m) FROM StudentGroupMember m WHERE m.groupId IN :groupIds GROUP BY m.groupId")
    List<Object[]> countByGroupIdIn(Collection<UUID> groupIds);
}
