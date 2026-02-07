package com.example.interhubdev.schedule;

import java.util.List;

/**
 * Port for enriching lessons with offering, slot and teachers in batch (no N+1).
 * Implemented by adapter using Offering module.
 */
public interface LessonEnrichmentPort {

    /**
     * Batch load offering summary, slot summary and teachers for each request.
     * Result list has the same size and order as requests; slot may be null if slotId was null or slot not found.
     *
     * @param requests one entry per lesson (offeringId, offeringSlotId)
     * @return enrichment for each request, same order
     */
    List<LessonEnrichmentData> getEnrichment(List<LessonEnrichmentRequest> requests);
}
