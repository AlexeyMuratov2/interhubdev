package com.example.interhubdev.offering;

import java.util.UUID;

/**
 * Key for batch lesson enrichment: offering id and optional slot id.
 *
 * @param offeringId offering the lesson belongs to
 * @param slotId     offering slot id, or null for legacy lessons
 */
public record OfferingSlotKey(UUID offeringId, UUID slotId) {
}
