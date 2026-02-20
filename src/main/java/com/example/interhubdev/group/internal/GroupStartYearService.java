package com.example.interhubdev.group.internal;

import com.example.interhubdev.group.GroupStartYearPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Internal service implementing GroupStartYearPort.
 * Depends only on repository to avoid circular dependencies.
 */
@Service
@RequiredArgsConstructor
class GroupStartYearService implements GroupStartYearPort {

    private final StudentGroupRepository studentGroupRepository;

    @Override
    public Optional<Integer> getStartYearByGroupId(UUID groupId) {
        return studentGroupRepository.findById(groupId)
                .map(StudentGroup::getStartYear);
    }
}
