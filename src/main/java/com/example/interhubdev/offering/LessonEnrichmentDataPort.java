package com.example.interhubdev.offering;

import java.util.List;

/**
 * Port for batch loading lesson enrichment (offering, slot, teachers) without N+1.
 * Implemented by a lightweight service that only uses repositories.
 */
public interface LessonEnrichmentDataPort {

    /**
     * Load offering, slot and teachers for each key. Result list has the same size and order as keys.
     * Slot is null if slotId was null or slot not found.
     *
     * @param keys one per lesson (offeringId, slotId)
     * @return enrichment for each key, same order
     */
    List<LessonEnrichmentItem> getEnrichmentBatch(List<OfferingSlotKey> keys);
}
