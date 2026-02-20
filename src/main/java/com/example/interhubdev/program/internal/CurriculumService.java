package com.example.interhubdev.program.internal;

import com.example.interhubdev.error.Errors;
import com.example.interhubdev.program.CurriculumDto;
import com.example.interhubdev.program.CurriculumStatus;
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
class CurriculumService {

    private final ProgramRepository programRepository;
    private final CurriculumRepository curriculumRepository;

    Optional<CurriculumDto> findCurriculumById(UUID id) {
        return curriculumRepository.findById(id).map(ProgramMappers::toCurriculumDto);
    }

    List<CurriculumDto> findCurriculaByProgramId(UUID programId) {
        return curriculumRepository.findByProgramIdOrderByVersionDesc(programId).stream()
                .map(ProgramMappers::toCurriculumDto)
                .toList();
    }

    @Transactional
    CurriculumDto createCurriculum(UUID programId, String version, int durationYears, boolean isActive, String notes) {
        if (programRepository.findById(programId).isEmpty()) {
            throw Errors.notFound("Program not found: " + programId);
        }

        String trimmedVersion = ProgramValidation.requiredTrimmed(version, "Curriculum version");
        ProgramValidation.validatePositive(durationYears, "durationYears");
        if (curriculumRepository.existsByProgramIdAndVersion(programId, trimmedVersion)) {
            throw Errors.conflict("Curriculum with version '" + trimmedVersion + "' already exists for program");
        }

        Curriculum entity = Curriculum.builder()
                .programId(programId)
                .version(trimmedVersion)
                .durationYears(durationYears)
                .isActive(isActive)
                .notes(notes != null ? notes.trim() : null)
                .build();
        return ProgramMappers.toCurriculumDto(curriculumRepository.save(entity));
    }

    @Transactional
    CurriculumDto updateCurriculum(UUID id, String version, int durationYears, boolean isActive, CurriculumStatus status, String notes) {
        Curriculum entity = curriculumRepository.findById(id)
                .orElseThrow(() -> Errors.notFound("Curriculum not found: " + id));

        if (version != null && version.isBlank()) {
            throw Errors.badRequest("Curriculum version must not be blank");
        }
        if (version != null) entity.setVersion(version.trim());

        ProgramValidation.validatePositive(durationYears, "durationYears");

        entity.setDurationYears(durationYears);
        entity.setActive(isActive);
        if (status != null) entity.setStatus(status);
        if (notes != null) entity.setNotes(notes.trim());
        entity.setUpdatedAt(LocalDateTime.now());
        return ProgramMappers.toCurriculumDto(curriculumRepository.save(entity));
    }

    @Transactional
    CurriculumDto approveCurriculum(UUID id, UUID approvedBy) {
        Curriculum entity = curriculumRepository.findById(id)
                .orElseThrow(() -> Errors.notFound("Curriculum not found: " + id));
        entity.setStatus(CurriculumStatus.APPROVED);
        entity.setApprovedAt(LocalDateTime.now());
        entity.setApprovedBy(approvedBy);
        entity.setUpdatedAt(LocalDateTime.now());
        return ProgramMappers.toCurriculumDto(curriculumRepository.save(entity));
    }

    @Transactional
    void deleteCurriculum(UUID id) {
        if (!curriculumRepository.existsById(id)) {
            throw Errors.notFound("Curriculum not found: " + id);
        }
        curriculumRepository.deleteById(id);
    }
}

