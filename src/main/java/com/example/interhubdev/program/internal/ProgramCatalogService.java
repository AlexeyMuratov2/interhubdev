package com.example.interhubdev.program.internal;

import com.example.interhubdev.department.DepartmentApi;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.program.ProgramDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class ProgramCatalogService {

    private final ProgramRepository programRepository;
    private final DepartmentApi departmentApi;

    Optional<ProgramDto> findProgramById(UUID id) {
        return programRepository.findById(id).map(ProgramMappers::toProgramDto);
    }

    Optional<ProgramDto> findProgramByCode(String code) {
        return programRepository.findByCode(code).map(ProgramMappers::toProgramDto);
    }

    List<ProgramDto> findAllPrograms() {
        return programRepository.findAllByOrderByCodeAsc().stream()
                .map(ProgramMappers::toProgramDto)
                .toList();
    }

    @Transactional
    ProgramDto createProgram(String code, String name, String description, String degreeLevel, UUID departmentId) {
        String trimmedCode = ProgramValidation.requiredTrimmed(code, "Program code");
        if (programRepository.existsByCode(trimmedCode)) {
            throw Errors.conflict("Program with code '" + trimmedCode + "' already exists");
        }
        if (departmentId != null && departmentApi.findById(departmentId).isEmpty()) {
            throw Errors.notFound("Department not found: " + departmentId);
        }
        Program entity = Program.builder()
                .code(trimmedCode)
                .name(name != null ? name.trim() : "")
                .description(description != null ? description.trim() : null)
                .degreeLevel(degreeLevel != null ? degreeLevel.trim() : null)
                .departmentId(departmentId)
                .build();
        return ProgramMappers.toProgramDto(programRepository.save(entity));
    }

    @Transactional
    ProgramDto updateProgram(UUID id, String name, String description, String degreeLevel, UUID departmentId) {
        Program entity = programRepository.findById(id)
                .orElseThrow(() -> Errors.notFound("Program not found: " + id));
        if (departmentId != null && departmentApi.findById(departmentId).isEmpty()) {
            throw Errors.notFound("Department not found: " + departmentId);
        }
        if (name != null) entity.setName(name.trim());
        if (description != null) entity.setDescription(description.trim());
        if (degreeLevel != null) entity.setDegreeLevel(degreeLevel.trim());
        entity.setDepartmentId(departmentId);
        entity.setUpdatedAt(LocalDateTime.now());
        return ProgramMappers.toProgramDto(programRepository.save(entity));
    }

    @Transactional
    void deleteProgram(UUID id) {
        if (!programRepository.existsById(id)) {
            throw Errors.notFound("Program not found: " + id);
        }
        programRepository.deleteById(id);
    }
}

