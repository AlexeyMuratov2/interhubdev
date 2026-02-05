package com.example.interhubdev.offering;

import java.util.UUID;

/**
 * Port for checking room existence. Implemented by the Schedule adapter so that
 * the Offering module does not depend on the Schedule module (dependency inversion).
 */
public interface RoomLookupPort {

    boolean existsById(UUID roomId);
}
