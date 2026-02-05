package com.example.interhubdev.adapter;

import com.example.interhubdev.offering.OfferingExistsPort;
import com.example.interhubdev.schedule.OfferingLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adapter: implements Schedule module's OfferingLookupPort using Offering module's OfferingExistsPort.
 * Uses minimal port (OfferingExistsPort) instead of OfferingApi to avoid circular dependency.
 */
@Component
@RequiredArgsConstructor
public class OfferingLookupAdapter implements OfferingLookupPort {

    private final OfferingExistsPort offeringExistsPort;

    @Override
    public boolean existsById(UUID offeringId) {
        return offeringExistsPort.existsById(offeringId);
    }
}
