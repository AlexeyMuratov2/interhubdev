package com.example.interhubdev.document.internal.homework;

import com.example.interhubdev.document.internal.storedFile.StoredFile;
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

import java.io.Serializable;
import java.util.UUID;

/**
 * JPA entity for junction table linking homework to stored files.
 * One homework has many files; each file has a sort_order for display order.
 */
@Entity
@Table(name = "homework_file")
@IdClass(HomeworkFile.HomeworkFileId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
class HomeworkFile {

    @Id
    @Column(name = "homework_id", nullable = false)
    private UUID homeworkId;

    @Id
    @Column(name = "stored_file_id", nullable = false)
    private UUID storedFileId;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homework_id", insertable = false, updatable = false)
    private Homework homework;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stored_file_id", insertable = false, updatable = false)
    private StoredFile storedFile;

    /**
     * Composite key for HomeworkFile entity.
     */
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    static class HomeworkFileId implements Serializable {
        private UUID homeworkId;
        private UUID storedFileId;
    }
}
