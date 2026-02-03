/**
 * Program module - programs, curricula, and curriculum subjects (Layer 1-2).
 *
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.program.ProgramApi} - programs, curricula, curriculum subjects</li>
 *   <li>{@link com.example.interhubdev.program.ProgramDto}, {@link com.example.interhubdev.program.CurriculumDto}, {@link com.example.interhubdev.program.CurriculumSubjectDto} - DTOs</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Program",
    allowedDependencies = {"department", "subject", "error"}
)
package com.example.interhubdev.program;
