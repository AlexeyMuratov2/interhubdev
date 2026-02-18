package com.example.interhubdev.group.internal;

import com.example.interhubdev.group.GroupSummaryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Internal service implementing GroupSummaryPort.
 * Depends only on repository to avoid circular dependencies.
 */
@Service
@RequiredArgsConstructor
class GroupSummaryService implements GroupSummaryPort {

    private final StudentGroupRepository studentGroupRepository;

    @Override
    public Map<UUID, GroupSummaryPort.GroupSummary> getGroupSummaries(List<UUID> groupIds) {
        if (groupIds == null || groupIds.isEmpty()) {
            return Map.of();
        }
        return studentGroupRepository.findAllById(groupIds).stream()
                .collect(Collectors.toMap(
                        StudentGroup::getId,
                        group -> new GroupSummaryPort.GroupSummary(
                                group.getId(),
                                group.getCode(),
                                group.getName()
                        )
                ));
    }
}
