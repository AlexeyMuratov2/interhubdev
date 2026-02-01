/**
 * Offering module - group subject offerings and offering teachers (Layer 3 delivery).
 *
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.offering.OfferingApi} - offerings and offering teachers</li>
 *   <li>{@link com.example.interhubdev.offering.GroupSubjectOfferingDto}, {@link com.example.interhubdev.offering.OfferingTeacherDto} - DTOs</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Offering",
    allowedDependencies = {"group", "program", "teacher", "schedule"}
)
package com.example.interhubdev.offering;
