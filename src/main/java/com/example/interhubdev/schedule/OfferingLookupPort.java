package com.example.interhubdev.schedule;

import java.util.UUID;

/**
 * Port for checking offering existence. Implemented by the Offering adapter so that
 * the Schedule module does not depend on the Offering module (dependency inversion).
 */
public interface OfferingLookupPort {

    boolean existsById(UUID offeringId);
}
