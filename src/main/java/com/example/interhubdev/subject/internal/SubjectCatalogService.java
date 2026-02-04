package com.example.interhubdev.subject.internal;

import com.example.interhubdev.department.DepartmentApi;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.subject.SubjectDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Internal service for subject catalog operations.
 * Handles CRUD for subjects and validates department reference when departmentId is set.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class SubjectCatalogService {

    private final SubjectRepository subjectRepository;
    private final DepartmentApi departmentApi;

    /**
     * Finds a subject by its unique id.
     *
     * @param id subject id (must not be null)
     * @return optional containing the subject DTO if found, empty otherwise
     */
    Optional<SubjectDto> findById(UUID id) {
        return subjectRepository.findById(id).map(SubjectMappers::toSubjectDto);
    }

    /**
     * Finds a subject by its unique code (case-sensitive after trim).
     *
     * @param code subject code (may be null; empty optional returned if null/blank)
     * @return optional containing the subject DTO if found, empty otherwise
     */
    Optional<SubjectDto> findByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return subjectRepository.findByCode(code.trim()).map(SubjectMappers::toSubjectDto);
    }

    /**
     * Returns all subjects ordered by code ascending for stable API responses.
     *
     * @return list of subject DTOs (never null, may be empty)
     */
    List<SubjectDto> findAll() {
        return subjectRepository.findAllByOrderByCodeAsc().stream()
                .map(SubjectMappers::toSubjectDto)
                .toList();
    }

    /**
     * Creates a new subject. Validates code (required, unique) and department (if provided).
     *
     * @param code        required, trimmed; must not already exist
     * @param chineseName required; trimmed, default empty string
     * @param englishName optional; trimmed, may be null
     * @param description optional; trimmed, may be null
     * @param departmentId optional; if non-null, department must exist
     * @return created subject DTO
     * @throws com.example.interhubdev.error.AppException BAD_REQUEST if code is blank
     * @throws com.example.interhubdev.error.AppException CONFLICT if subject with code already exists
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if departmentId is set and department not found
     */
    @Transactional
    SubjectDto create(String code, String chineseName, String englishName, String description, UUID departmentId) {
        String trimmedCode = SubjectValidation.requireTrimmedCode(code);
        if (subjectRepository.existsByCode(trimmedCode)) {
            throw Errors.conflict("Subject with code '" + trimmedCode + "' already exists");
        }
        if (departmentId != null && departmentApi.findById(departmentId).isEmpty()) {
            throw Errors.notFound("Department not found: " + departmentId);
        }
        Subject entity = Subject.builder()
                .code(trimmedCode)
                .chineseName(SubjectValidation.trimChineseName(chineseName))
                .englishName(SubjectValidation.trimEnglishName(englishName))
                .description(SubjectValidation.trimDescription(description))
                .departmentId(departmentId)
                .build();
        return SubjectMappers.toSubjectDto(subjectRepository.save(entity));
    }

    /**
     * Updates an existing subject by id. Validates subject exists and department (if provided).
     *
     * @param id          subject id (must not be null)
     * @param chineseName optional; if non-null, trimmed and set
     * @param englishName optional; if non-null, trimmed and set (use null in request to leave unchanged)
     * @param description optional; if non-null, trimmed and set (may set to null)
     * @param departmentId optional; if non-null, department must exist
     * @return updated subject DTO
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if subject or (when set) department not found
     */
    @Transactional
    SubjectDto update(UUID id, String chineseName, String englishName, String description, UUID departmentId) {
        Subject entity = subjectRepository.findById(id)
                .orElseThrow(() -> Errors.notFound("Subject not found: " + id));
        if (departmentId != null && departmentApi.findById(departmentId).isEmpty()) {
            throw Errors.notFound("Department not found: " + departmentId);
        }
        if (chineseName != null) entity.setChineseName(SubjectValidation.trimChineseName(chineseName));
        if (englishName != null) entity.setEnglishName(SubjectValidation.trimEnglishName(englishName));
        if (description != null) entity.setDescription(SubjectValidation.trimDescription(description));
        if (departmentId != null) entity.setDepartmentId(departmentId);
        entity.setUpdatedAt(LocalDateTime.now());
        return SubjectMappers.toSubjectDto(subjectRepository.save(entity));
    }

    /**
     * Deletes a subject by id.
     *
     * @param id subject id (must not be null)
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if subject does not exist
     */
    @Transactional
    void delete(UUID id) {
        if (!subjectRepository.existsById(id)) {
            throw Errors.notFound("Subject not found: " + id);
        }
        subjectRepository.deleteById(id);
    }
}
