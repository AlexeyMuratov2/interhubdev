package com.example.interhubdev.department.internal;

import com.example.interhubdev.department.DepartmentApi;
import com.example.interhubdev.department.DepartmentDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    @Operation(summary = "Create department")
    public ResponseEntity<DepartmentDto> create(@RequestBody CreateDepartmentRequest request) {
        DepartmentDto dto = departmentApi.create(
                request.code(),
                request.name(),
                request.description()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update department")
    public ResponseEntity<DepartmentDto> update(
            @PathVariable UUID id,
            @RequestBody UpdateDepartmentRequest request
    ) {
        DepartmentDto dto = departmentApi.update(id, request.name(), request.description());
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete department")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        departmentApi.delete(id);
        return ResponseEntity.noContent().build();
    }

    record CreateDepartmentRequest(String code, String name, String description) {}
    record UpdateDepartmentRequest(String name, String description) {}
}
