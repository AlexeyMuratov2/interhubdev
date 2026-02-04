package com.example.interhubdev.subject.internal;

import com.example.interhubdev.subject.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Facade implementing {@link SubjectApi}. Delegates to internal catalog services
 * for subjects and assessment types so that business logic stays in dedicated services.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class SubjectServiceImpl implements SubjectApi {

    private final SubjectCatalogService subjectCatalogService;
    private final AssessmentTypeCatalogService assessmentTypeCatalogService;

    // --- Subject ---

    /**
     * {@inheritDoc}
     * Delegates to {@link SubjectCatalogService#findById(UUID)}.
     */
    @Override
    public Optional<SubjectDto> findSubjectById(UUID id) {
        return subjectCatalogService.findById(id);
    }

    /**
     * {@inheritDoc}
     * Delegates to {@link SubjectCatalogService#findByCode(String)}.
     */
    @Override
    public Optional<SubjectDto> findSubjectByCode(String code) {
        return subjectCatalogService.findByCode(code);
    }

    /**
     * {@inheritDoc}
     * Delegates to {@link SubjectCatalogService#findAll()}. Order is stable (by code).
     */
    @Override
    public List<SubjectDto> findAllSubjects() {
        return subjectCatalogService.findAll();
    }

    /**
     * {@inheritDoc}
     * Delegates to {@link SubjectCatalogService#create(String, String, String, String, UUID)}.
     * Validates code and department; throws CONFLICT if code already exists.
     */
    @Override
    @Transactional
    public SubjectDto createSubject(String code, String chineseName, String englishName, String description, UUID departmentId) {
        return subjectCatalogService.create(code, chineseName, englishName, description, departmentId);
    }

    /**
     * {@inheritDoc}
     * Delegates to {@link SubjectCatalogService#update(UUID, String, String, String, UUID)}.
     * Throws NOT_FOUND if subject or (when set) department does not exist.
     */
    @Override
    @Transactional
    public SubjectDto updateSubject(UUID id, String chineseName, String englishName, String description, UUID departmentId) {
        return subjectCatalogService.update(id, chineseName, englishName, description, departmentId);
    }

    /**
     * {@inheritDoc}
     * Delegates to {@link SubjectCatalogService#delete(UUID)}. Throws NOT_FOUND if subject does not exist.
     */
    @Override
    @Transactional
    public void deleteSubject(UUID id) {
        subjectCatalogService.delete(id);
    }

    // --- Assessment type ---

    /**
     * {@inheritDoc}
     * Delegates to {@link AssessmentTypeCatalogService#findById(UUID)}.
     */
    @Override
    public Optional<AssessmentTypeDto> findAssessmentTypeById(UUID id) {
        return assessmentTypeCatalogService.findById(id);
    }

    /**
     * {@inheritDoc}
     * Delegates to {@link AssessmentTypeCatalogService#findByCode(String)}.
     */
    @Override
    public Optional<AssessmentTypeDto> findAssessmentTypeByCode(String code) {
        return assessmentTypeCatalogService.findByCode(code);
    }

    /**
     * {@inheritDoc}
     * Delegates to {@link AssessmentTypeCatalogService#findAll()}. Order is stable (sort order, then code).
     */
    @Override
    public List<AssessmentTypeDto> findAllAssessmentTypes() {
        return assessmentTypeCatalogService.findAll();
    }

    /**
     * {@inheritDoc}
     * Delegates to {@link AssessmentTypeCatalogService#create(String, String, String, Boolean, Boolean, Integer)}.
     * Validates code; throws CONFLICT if code already exists.
     */
    @Override
    @Transactional
    public AssessmentTypeDto createAssessmentType(String code, String chineseName, String englishName, Boolean isGraded, Boolean isFinal, Integer sortOrder) {
        return assessmentTypeCatalogService.create(code, chineseName, englishName, isGraded, isFinal, sortOrder);
    }

    /**
     * {@inheritDoc}
     * Delegates to {@link AssessmentTypeCatalogService#update(UUID, String, String, Boolean, Boolean, Integer)}.
     */
    @Override
    @Transactional
    public AssessmentTypeDto updateAssessmentType(UUID id, String chineseName, String englishName, Boolean isGraded, Boolean isFinal, Integer sortOrder) {
        return assessmentTypeCatalogService.update(id, chineseName, englishName, isGraded, isFinal, sortOrder);
    }

    /**
     * {@inheritDoc}
     * Delegates to {@link AssessmentTypeCatalogService#delete(UUID)}. Throws NOT_FOUND if assessment type does not exist.
     */
    @Override
    @Transactional
    public void deleteAssessmentType(UUID id) {
        assessmentTypeCatalogService.delete(id);
    }
}
