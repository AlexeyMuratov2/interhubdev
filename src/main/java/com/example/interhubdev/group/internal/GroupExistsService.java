package com.example.interhubdev.group.internal;

import com.example.interhubdev.group.GroupExistsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Internal service implementing GroupExistsPort.
 * Depends only on repository to avoid circular dependencies.
 */
@Service
@RequiredArgsConstructor
class GroupExistsService implements GroupExistsPort {

    private final StudentGroupRepository studentGroupRepository;

    @Override
    public boolean existsById(UUID groupId) {
        return studentGroupRepository.existsById(groupId);
    }
}
