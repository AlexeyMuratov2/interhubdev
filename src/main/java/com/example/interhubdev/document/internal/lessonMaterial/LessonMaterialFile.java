package com.example.interhubdev.document.internal.lessonMaterial;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.example.interhubdev.document.internal.storedFile.StoredFile;

import java.io.Serializable;
import java.util.UUID;

/**
 * JPA entity for junction table linking lesson materials to stored files.
 * One material has many files; each file has a sort_order for display order.
 */
@Entity
@Table(name = "lesson_material_file")
@IdClass(LessonMaterialFile.LessonMaterialFileId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
class LessonMaterialFile {

    @Id
    @Column(name = "lesson_material_id", nullable = false)
    private UUID lessonMaterialId;

    @Id
    @Column(name = "stored_file_id", nullable = false)
    private UUID storedFileId;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_material_id", insertable = false, updatable = false)
    private LessonMaterial lessonMaterial;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stored_file_id", insertable = false, updatable = false)
    private StoredFile storedFile;

    /**
     * Composite key for LessonMaterialFile entity.
     */
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    static class LessonMaterialFileId implements Serializable {
        private UUID lessonMaterialId;
        private UUID storedFileId;
    }
}
