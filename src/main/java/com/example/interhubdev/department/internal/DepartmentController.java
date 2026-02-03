package com.example.interhubdev.department.internal;

import com.example.interhubdev.department.DepartmentApi;
import com.example.interhubdev.department.DepartmentDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
@Tag(name = "Departments", description = "Department/faculty catalog")
class DepartmentController {

    private final DepartmentApi departmentApi;

    @GetMapping
    @Operation(summary = "Get all departments")
    public ResponseEntity<List<DepartmentDto>> findAll() {
        return ResponseEntity.ok(departmentApi.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get department by ID")
    public ResponseEntity<DepartmentDto> findById(@PathVariable UUID id) {
        return departmentApi.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get department by code")
    public ResponseEntity<DepartmentDto> findByCode(@PathVariable String code) {
        return departmentApi.findByCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create department", description = "Only STAFF, ADMIN, SUPER_ADMIN can create departments")
    public ResponseEntity<DepartmentDto> create(@Valid @RequestBody CreateDepartmentRequest request) {
        DepartmentDto dto = departmentApi.create(
                request.code(),
                request.name(),
                request.description()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update department", description = "Only STAFF, ADMIN, SUPER_ADMIN can update departments")
    public ResponseEntity<DepartmentDto> update(
            @PathVariable UUID id,
            @RequestBody UpdateDepartmentRequest request
    ) {
        DepartmentDto dto = departmentApi.update(id, request.name(), request.description());
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete department", description = "Only STAFF, ADMIN, SUPER_ADMIN can delete departments")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        departmentApi.delete(id);
        return ResponseEntity.noContent().build();
    }

    record CreateDepartmentRequest(
            @jakarta.validation.constraints.NotBlank(message = "Code is required")
            String code,
            @jakarta.validation.constraints.NotBlank(message = "Name is required")
            String name,
            String description
    ) {}
    record UpdateDepartmentRequest(String name, String description) {}
}
