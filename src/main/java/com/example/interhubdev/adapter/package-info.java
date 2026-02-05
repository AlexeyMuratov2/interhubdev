/**
 * Adapters that connect Schedule and Offering modules without creating a circular dependency.
 *
 * <h2>Purpose</h2>
 * <ul>
 *   <li>Schedule needs to validate that an offering exists when creating a lesson.</li>
 *   <li>Offering needs to validate that a room exists when creating/updating an offering (rooms live in Schedule).</li>
 * </ul>
 * Direct dependency Schedule ↔ Offering would be circular. This package implements the consumer ports
 * of each module by delegating to the other module's "exists" ports.
 *
 * <h2>Adapters</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.adapter.OfferingLookupAdapter} - implements Schedule's
 *       {@link com.example.interhubdev.schedule.OfferingLookupPort} using Offering's
 *       {@link com.example.interhubdev.offering.OfferingExistsPort}.</li>
 *   <li>{@link com.example.interhubdev.adapter.ScheduleRoomLookupAdapter} - implements Offering's
 *       {@link com.example.interhubdev.offering.RoomLookupPort} using Schedule's
 *       {@link com.example.interhubdev.schedule.RoomExistsPort}.</li>
 * </ul>
 *
 * <h2>Dependencies</h2>
 * This package depends on both {@code schedule} and {@code offering} (only their port interfaces
 * and types used in method signatures). The modules themselves do not depend on each other.
 */
package com.example.interhubdev.adapter;
