/**
 * Adapters that connect modules through lightweight public ports without
 * creating circular dependencies.
 *
 * <p>This package is intentionally thin: it wires one module's public port to
 * another module's public API and does not own business logic.</p>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Adapter",
    allowedDependencies = {"schedule", "offering", "group", "group :: port", "document", "subject :: port", "program", "academic", "submission", "notification", "student", "teacher", "user", "error"}
)
package com.example.interhubdev.adapter;
