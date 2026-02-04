package com.example.interhubdev.subject.internal;

import com.example.interhubdev.error.Errors;
import com.example.interhubdev.subject.AssessmentTypeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Internal service for assessment type catalog operations.
 * Handles CRUD for assessment types (e.g. exam, test, coursework) with code uniqueness and defaults.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class AssessmentTypeCatalogService {

    private final AssessmentTypeRepository assessmentTypeRepository;

    /**
     * Finds an assessment type by its unique id.
     *
     * @param id assessment type id (must not be null)
     * @return optional containing the assessment type DTO if found, empty otherwise
     */
    Optional<AssessmentTypeDto> findById(UUID id) {
        return assessmentTypeRepository.findById(id).map(SubjectMappers::toAssessmentTypeDto);
    }

    /**
     * Finds an assessment type by its unique code (case-sensitive after trim).
     *
     * @param code assessment type code (may be null; empty optional returned if null/blank)
     * @return optional containing the assessment type DTO if found, empty otherwise
     */
    Optional<AssessmentTypeDto> findByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return assessmentTypeRepository.findByCode(code.trim()).map(SubjectMappers::toAssessmentTypeDto);
    }

    /**
     * Returns all assessment types ordered by sort order then code for stable API responses (e.g. dropdowns).
     *
     * @return list of assessment type DTOs (never null, may be empty)
     */
    List<AssessmentTypeDto> findAll() {
        return assessmentTypeRepository.findAllByOrderBySortOrderAscCodeAsc().stream()
                .map(SubjectMappers::toAssessmentTypeDto)
                .toList();
    }

    /**
     * Creates a new assessment type. Validates code (required, unique). Applies defaults for optional fields.
     *
     * @param code        required, trimmed; must not already exist
     * @param chineseName required; trimmed, default empty string
     * @param englishName optional; trimmed, may be null
     * @param isGraded    optional; default true
     * @param isFinal     optional; default false
     * @param sortOrder   optional; default 0
     * @return created assessment type DTO
     * @throws com.example.interhubdev.error.AppException BAD_REQUEST if code is blank
     * @throws com.example.interhubdev.error.AppException CONFLICT if assessment type with code already exists
     */
    @Transactional
    AssessmentTypeDto create(String code, String chineseName, String englishName, Boolean isGraded, Boolean isFinal, Integer sortOrder) {
        String trimmedCode = SubjectValidation.requireTrimmedAssessmentTypeCode(code);
        if (assessmentTypeRepository.existsByCode(trimmedCode)) {
            throw Errors.conflict("Assessment type with code '" + trimmedCode + "' already exists");
        }
        AssessmentType entity = AssessmentType.builder()
                .code(trimmedCode)
                .chineseName(SubjectValidation.trimChineseName(chineseName))
                .englishName(SubjectValidation.trimEnglishName(englishName))
                .isGraded(isGraded != null ? isGraded : true)
                .isFinal(isFinal != null ? isFinal : false)
                .sortOrder(sortOrder != null ? sortOrder : 0)
                .build();
        return SubjectMappers.toAssessmentTypeDto(assessmentTypeRepository.save(entity));
    }

    /**
     * Updates an existing assessment type by id.
     *
     * @param id          assessment type id (must not be null)
     * @param chineseName optional; if non-null, trimmed and set
     * @param englishName optional; if non-null, trimmed and set
     * @param isGraded    optional; if non-null, set
     * @param isFinal     optional; if non-null, set
     * @param sortOrder   optional; if non-null, set
     * @return updated assessment type DTO
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if assessment type does not exist
     */
    @Transactional
    AssessmentTypeDto update(UUID id, String chineseName, String englishName, Boolean isGraded, Boolean isFinal, Integer sortOrder) {
        AssessmentType entity = assessmentTypeRepository.findById(id)
                .orElseThrow(() -> Errors.notFound("Assessment type not found: " + id));
        if (chineseName != null) entity.setChineseName(SubjectValidation.trimChineseName(chineseName));
        if (englishName != null) entity.setEnglishName(SubjectValidation.trimEnglishName(englishName));
        if (isGraded != null) entity.setIsGraded(isGraded);
        if (isFinal != null) entity.setIsFinal(isFinal);
        if (sortOrder != null) entity.setSortOrder(sortOrder);
        return SubjectMappers.toAssessmentTypeDto(assessmentTypeRepository.save(entity));
    }

    /**
     * Deletes an assessment type by id.
     *
     * @param id assessment type id (must not be null)
     * @throws com.example.interhubdev.error.AppException NOT_FOUND if assessment type does not exist
     */
    @Transactional
    void delete(UUID id) {
        if (!assessmentTypeRepository.existsById(id)) {
            throw Errors.notFound("Assessment type not found: " + id);
        }
        assessmentTypeRepository.deleteById(id);
    }
}
