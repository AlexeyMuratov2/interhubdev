package com.example.interhubdev.adapter;

import com.example.interhubdev.group.GroupExistsPort;
import com.example.interhubdev.offering.GroupLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adapter: implements Offering's GroupLookupPort using Group module's GroupExistsPort.
 * Uses lightweight port that only checks existence, avoiding circular dependencies.
 */
@Component
@RequiredArgsConstructor
public class GroupLookupAdapterForOffering implements GroupLookupPort {

    private final GroupExistsPort groupExistsPort;

    @Override
    public boolean existsById(UUID groupId) {
        return groupExistsPort.existsById(groupId);
    }
}
