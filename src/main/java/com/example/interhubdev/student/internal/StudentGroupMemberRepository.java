package com.example.interhubdev.student.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface StudentGroupMemberRepository extends JpaRepository<StudentGroupMember, UUID> {

    List<StudentGroupMember> findByGroupId(UUID groupId);

    List<StudentGroupMember> findByStudentId(UUID studentId);

    boolean existsByStudentIdAndGroupId(UUID studentId, UUID groupId);

    void deleteByStudentIdAndGroupId(UUID studentId, UUID groupId);
}
