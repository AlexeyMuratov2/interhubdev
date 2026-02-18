package com.example.interhubdev.adapter;

import com.example.interhubdev.document.OfferingLookupPort;
import com.example.interhubdev.offering.OfferingExistsPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adapter: implements Document module's OfferingLookupPort using Offering module's OfferingExistsPort.
 * Allows course materials to validate offering id without document depending on offering.
 */
@Component
@RequiredArgsConstructor
public class OfferingLookupAdapterForDocument implements OfferingLookupPort {

    private final OfferingExistsPort offeringExistsPort;

    @Override
    public boolean existsById(UUID offeringId) {
        return offeringExistsPort.existsById(offeringId);
    }
}
