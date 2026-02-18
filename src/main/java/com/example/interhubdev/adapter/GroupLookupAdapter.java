package com.example.interhubdev.adapter;

import com.example.interhubdev.group.GroupExistsPort;
import com.example.interhubdev.group.GroupSummaryPort;
import com.example.interhubdev.schedule.GroupLookupPort;
import com.example.interhubdev.schedule.GroupSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adapter: implements Schedule's GroupLookupPort using Group module's lightweight ports.
 * Uses GroupExistsPort for existence check and GroupSummaryPort for summaries to avoid circular dependencies.
 */
@Component
@RequiredArgsConstructor
public class GroupLookupAdapter implements GroupLookupPort {

    private final GroupExistsPort groupExistsPort;
    private final GroupSummaryPort groupSummaryPort;

    @Override
    public boolean existsById(UUID groupId) {
        return groupExistsPort.existsById(groupId);
    }

    @Override
    public Map<UUID, GroupSummaryDto> getGroupSummaries(List<UUID> groupIds) {
        Map<UUID, GroupSummaryPort.GroupSummary> summaries = groupSummaryPort.getGroupSummaries(groupIds);
        return summaries.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            GroupSummaryPort.GroupSummary summary = entry.getValue();
                            return new GroupSummaryDto(summary.id(), summary.code(), summary.name());
                        }
                ));
    }
}
