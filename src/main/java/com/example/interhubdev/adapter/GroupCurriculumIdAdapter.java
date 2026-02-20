package com.example.interhubdev.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter: implements Program's GroupCurriculumIdPort using Group module's lightweight GroupCurriculumIdPort.
 * Uses lightweight port to avoid circular dependencies.
 */
@Component
@RequiredArgsConstructor
public class GroupCurriculumIdAdapter implements com.example.interhubdev.program.GroupCurriculumIdPort {

    private final com.example.interhubdev.group.GroupCurriculumIdPort groupCurriculumIdPort;

    @Override
    public Optional<UUID> getCurriculumIdByGroupId(UUID groupId) {
        return groupCurriculumIdPort.getCurriculumIdByGroupId(groupId);
    }
}
