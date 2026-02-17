/**
 * Adapters that connect Schedule and Offering modules without creating a circular dependency.
 *
 * <h2>Purpose</h2>
 * <ul>
 *   <li>Schedule needs to validate that an offering exists when creating a lesson.</li>
 *   <li>Offering needs to validate that a room exists when creating/updating an offering (rooms live in Schedule).</li>
 *   <li>Offering needs timeslot info (day of week) for lesson generation.</li>
 *   <li>Offering needs to create/delete lessons in bulk for lesson generation.</li>
 * </ul>
 * Direct dependency Schedule ↔ Offering would be circular. This package implements the consumer ports
 * of each module by delegating to the other module's ports or APIs.
 *
 * <h2>Adapters</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.adapter.OfferingLookupAdapter} - implements Schedule's
 *       {@link com.example.interhubdev.schedule.OfferingLookupPort} using Offering's
 *       {@link com.example.interhubdev.offering.OfferingLookupDataPort} (minimal, repo-only; avoids cycle).</li>
 *   <li>{@link com.example.interhubdev.adapter.GroupLookupAdapter} - implements Schedule's
 *       {@link com.example.interhubdev.schedule.GroupLookupPort} using {@link com.example.interhubdev.group.GroupApi}
 *       (so GET /lessons/group/{id} returns 404 when group does not exist).</li>
 *   <li>{@link com.example.interhubdev.adapter.ScheduleRoomLookupAdapter} - implements Offering's
 *       {@link com.example.interhubdev.offering.RoomLookupPort} using Schedule's
 *       {@link com.example.interhubdev.schedule.RoomExistsPort}.</li>
 *   <li>{@link com.example.interhubdev.adapter.ScheduleTimeslotLookupAdapter} - implements Offering's
 *       {@link com.example.interhubdev.offering.TimeslotLookupPort} using Schedule's
 *       {@link com.example.interhubdev.schedule.ScheduleApi}.</li>
 *   <li>{@link com.example.interhubdev.adapter.ScheduleLessonCreationAdapter} - implements Offering's
 *       {@link com.example.interhubdev.offering.LessonCreationPort} using Schedule's
 *       {@link com.example.interhubdev.schedule.ScheduleApi}.</li>
 *   <li>{@link com.example.interhubdev.adapter.LessonEnrichmentAdapter} - implements Schedule's
 *       {@link com.example.interhubdev.schedule.LessonEnrichmentPort} using Offering's
 *       {@link com.example.interhubdev.offering.LessonEnrichmentDataPort}.</li>
 *   <li>{@link com.example.interhubdev.adapter.LessonLookupAdapter} - implements Document's
 *       {@link com.example.interhubdev.document.LessonLookupPort} using Schedule's
 *       {@link com.example.interhubdev.schedule.ScheduleApi} (for homework lesson validation).</li>
 * </ul>
 *
 * <h2>Dependencies</h2>
 * This package depends on both {@code schedule} and {@code offering} (only their port interfaces
 * and types used in method signatures). The modules themselves do not depend on each other.
 */
package com.example.interhubdev.adapter;
