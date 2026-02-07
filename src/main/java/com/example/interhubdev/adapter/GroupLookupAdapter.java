package com.example.interhubdev.adapter;

import com.example.interhubdev.group.GroupApi;
import com.example.interhubdev.schedule.GroupLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adapter: implements Schedule's GroupLookupPort using Group module's GroupApi.
 * Allows Schedule to return 404 when group does not exist (e.g. GET /lessons/group/{groupId}).
 */
@Component
@RequiredArgsConstructor
public class GroupLookupAdapter implements GroupLookupPort {

    private final GroupApi groupApi;

    @Override
    public boolean existsById(UUID groupId) {
        return groupApi.findGroupById(groupId).isPresent();
    }
}
