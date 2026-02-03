package com.example.interhubdev.department.internal;

import com.example.interhubdev.department.DepartmentApi;
import com.example.interhubdev.department.DepartmentDto;
import com.example.interhubdev.error.Errors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class DepartmentServiceImpl implements DepartmentApi {

    private final DepartmentRepository departmentRepository;

    @Override
    public Optional<DepartmentDto> findById(UUID id) {
        return departmentRepository.findById(id).map(this::toDto);
    }

    @Override
    public Optional<DepartmentDto> findByCode(String code) {
        return departmentRepository.findByCode(code).map(this::toDto);
    }

    @Override
    public List<DepartmentDto> findAll() {
        return departmentRepository.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public DepartmentDto create(String code, String name, String description) {
        if (code == null || code.isBlank()) {
            throw Errors.badRequest("Department code is required");
        }
        if (departmentRepository.existsByCode(code.trim())) {
            throw Errors.conflict("Department with code '" + code + "' already exists");
        }
        Department entity = Department.builder()
                .code(code.trim())
                .name(name != null ? name.trim() : "")
                .description(description != null ? description.trim() : null)
                .build();
        return toDto(departmentRepository.save(entity));
    }

    @Override
    @Transactional
    public DepartmentDto update(UUID id, String name, String description) {
        Department entity = departmentRepository.findById(id)
                .orElseThrow(() -> Errors.notFound("Department not found: " + id));
        if (name != null) {
            entity.setName(name.trim());
        }
        if (description != null) {
            entity.setDescription(description.trim());
        }
        return toDto(departmentRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        if (!departmentRepository.existsById(id)) {
            throw Errors.notFound("Department not found: " + id);
        }
        departmentRepository.deleteById(id);
    }

    private DepartmentDto toDto(Department e) {
        return new DepartmentDto(e.getId(), e.getCode(), e.getName(), e.getDescription(), e.getCreatedAt());
    }
}
