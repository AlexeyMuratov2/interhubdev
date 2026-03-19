package com.example.interhubdev.document.internal.storedFile;

import com.example.interhubdev.document.DocumentStoredFileUsagePort;
import com.example.interhubdev.document.internal.courseMaterial.CourseMaterialRepository;
import com.example.interhubdev.document.internal.homework.HomeworkFileRepository;
import com.example.interhubdev.document.internal.lessonMaterial.LessonMaterialFileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of {@link DocumentStoredFileUsagePort} using only document repositories.
 * Does not depend on StoredFileApi, so adapters can use this port without creating a cycle with storedfile.
 */
@Service
class DocumentStoredFileUsageService implements DocumentStoredFileUsagePort {

    private final CourseMaterialRepository courseMaterialRepository;
    private final HomeworkFileRepository homeworkFileRepository;
    private final LessonMaterialFileRepository lessonMaterialFileRepository;

    DocumentStoredFileUsageService(
        CourseMaterialRepository courseMaterialRepository,
        HomeworkFileRepository homeworkFileRepository,
        LessonMaterialFileRepository lessonMaterialFileRepository
    ) {
        this.courseMaterialRepository = courseMaterialRepository;
        this.homeworkFileRepository = homeworkFileRepository;
        this.lessonMaterialFileRepository = lessonMaterialFileRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isStoredFileInUse(UUID storedFileId) {
        return courseMaterialRepository.existsByStoredFileId(storedFileId)
            || homeworkFileRepository.existsByStoredFileId(storedFileId)
            || lessonMaterialFileRepository.existsByStoredFileId(storedFileId);
    }
}
