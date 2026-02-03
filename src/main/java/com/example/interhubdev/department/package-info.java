/**
 * Department module - catalog of departments/faculties.
 *
 * <h2>Public API</h2>
 * <ul>
 *   <li>{@link com.example.interhubdev.department.DepartmentApi} - main interface</li>
 *   <li>{@link com.example.interhubdev.department.DepartmentDto} - department DTO</li>
 * </ul>
 *
 * <h2>Access control</h2>
 * Create, update and delete operations are allowed only for roles: STAFF, ADMIN, SUPER_ADMIN.
 * Teachers and students can only read departments.
 *
 * <h2>Error codes (via {@link com.example.interhubdev.error.Errors})</h2>
 * <ul>
 *   <li>NOT_FOUND (404) - department not found by id</li>
 *   <li>CONFLICT (409) - department with given code already exists</li>
 *   <li>BAD_REQUEST (400) - code is blank or invalid input</li>
 *   <li>VALIDATION_FAILED (400) - request validation failed (e.g. @Valid on create)</li>
 *   <li>FORBIDDEN (403) - user has no STAFF/ADMIN/SUPER_ADMIN role for write operations</li>
 * </ul>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Department",
    allowedDependencies = {"error"}
)
package com.example.interhubdev.department;
