package com.example.interhubdev.document.internal.storedFile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity for file metadata stored in object storage (S3/MinIO).
 * No business context (homework, lesson material) — that will be in Document later.
 */
@Entity
@Table(name = "stored_file")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoredFile {

    @Id
    private UUID id;

    @Column(name = "storage_path", nullable = false, unique = true, length = 1024)
    private String storagePath;

    @Column(name = "size", nullable = false)
    private long size;

    @Column(name = "content_type", length = 255)
    private String contentType;

    @Column(name = "original_name", length = 512)
    private String originalName;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @Column(name = "uploaded_by")
    private UUID uploadedBy;
}
