package com.example.interhubdev.document.internal.courseMaterial;

import com.example.interhubdev.document.CourseMaterialDto;
import com.example.interhubdev.document.DocumentAttachmentDto;

/**
 * Entity to DTO mapping for course materials. No instantiation.
 */
final class CourseMaterialMappers {

    private CourseMaterialMappers() {
    }

    static CourseMaterialDto toDto(CourseMaterial entity, DocumentAttachmentDto attachment) {
        return new CourseMaterialDto(
            entity.getId(),
            entity.getOfferingId(),
            entity.getTitle(),
            entity.getDescription(),
            entity.getAuthorId(),
            entity.getUploadedAt(),
            attachment
        );
    }
}
