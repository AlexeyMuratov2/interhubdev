package com.example.interhubdev.department;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Public API for the Department module.
 * Manages department/faculty catalog.
 */
public interface DepartmentApi {

    Optional<DepartmentDto> findById(UUID id);

    Optional<DepartmentDto> findByCode(String code);

    List<DepartmentDto> findAll();

    DepartmentDto create(String code, String name, String description);

    DepartmentDto update(UUID id, String name, String description);

    void delete(UUID id);
}
