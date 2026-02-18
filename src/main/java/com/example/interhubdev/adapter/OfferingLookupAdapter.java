package com.example.interhubdev.adapter;

import com.example.interhubdev.offering.OfferingLookupDataPort;
import com.example.interhubdev.schedule.OfferingLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Adapter: implements Schedule module's OfferingLookupPort using Offering module's OfferingLookupDataPort.
 * Uses only the minimal port (repository-backed) to avoid circular dependency with Schedule.
 */
@Component
@RequiredArgsConstructor
public class OfferingLookupAdapter implements OfferingLookupPort {

    private final OfferingLookupDataPort offeringLookupDataPort;

    @Override
    public boolean existsById(UUID offeringId) {
        return offeringLookupDataPort.existsById(offeringId);
    }

    @Override
    public List<UUID> findOfferingIdsByGroupId(UUID groupId) {
        return offeringLookupDataPort.findOfferingIdsByGroupId(groupId);
    }

    @Override
    public List<UUID> findOfferingIdsByTeacherId(UUID teacherId) {
        return offeringLookupDataPort.findOfferingIdsByTeacherId(teacherId);
    }
}
