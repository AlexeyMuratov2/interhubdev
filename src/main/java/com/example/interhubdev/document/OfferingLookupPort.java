package com.example.interhubdev.document;

import java.util.UUID;

/**
 * Port for checking offering existence without depending on offering module.
 * Implemented by adapter that delegates to {@link com.example.interhubdev.offering.OfferingExistsPort}.
 */
public interface OfferingLookupPort {

    /**
     * Check if an offering exists by id.
     *
     * @param offeringId offering UUID
     * @return true if offering exists
     */
    boolean existsById(UUID offeringId);
}
