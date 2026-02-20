package com.example.interhubdev.group.internal;

import com.example.interhubdev.group.GroupCurriculumIdPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Internal service implementing GroupCurriculumIdPort.
 * Depends only on repository to avoid circular dependencies.
 */
@Service
@RequiredArgsConstructor
class GroupCurriculumIdService implements GroupCurriculumIdPort {

    private final StudentGroupRepository studentGroupRepository;

    @Override
    public Optional<UUID> getCurriculumIdByGroupId(UUID groupId) {
        return studentGroupRepository.findById(groupId)
                .map(StudentGroup::getCurriculumId);
    }
}
