package com.example.interhubdev.offering;

import java.util.UUID;

/**
 * Port exposed by Offering module: check offering existence without pulling full OfferingApi.
 * Implemented by OfferingCatalogService; used by adapters to avoid circular dependency.
 */
public interface OfferingExistsPort {

    boolean existsById(UUID offeringId);
}
