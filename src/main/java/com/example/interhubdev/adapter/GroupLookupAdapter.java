package com.example.interhubdev.adapter;

import com.example.interhubdev.group.GroupApi;
import com.example.interhubdev.group.StudentGroupDto;
import com.example.interhubdev.schedule.GroupLookupPort;
import com.example.interhubdev.schedule.GroupSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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

    @Override
    public Map<UUID, GroupSummaryDto> getGroupSummaries(List<UUID> groupIds) {
        return groupIds.stream()
                .map(groupApi::findGroupById)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .collect(Collectors.toMap(
                        StudentGroupDto::id,
                        dto -> new GroupSummaryDto(dto.id(), dto.code(), dto.name())
                ));
    }
}
