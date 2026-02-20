package com.example.interhubdev.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Adapter: implements Program's GroupStartYearPort using Group module's lightweight GroupStartYearPort.
 * Uses lightweight port to avoid circular dependencies.
 */
@Component
@RequiredArgsConstructor
public class GroupStartYearAdapter implements com.example.interhubdev.program.GroupStartYearPort {

    private final com.example.interhubdev.group.GroupStartYearPort groupStartYearPort;

    @Override
    public Optional<Integer> getStartYearByGroupId(UUID groupId) {
        return groupStartYearPort.getStartYearByGroupId(groupId);
    }
}
