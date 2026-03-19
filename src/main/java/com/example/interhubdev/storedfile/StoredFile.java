package com.example.interhubdev.storedfile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * JPA entity for file metadata in object storage (S3/MinIO). No business context.
 * Lifecycle: status (activation gate); only ACTIVE allows bind/download. DELETED is terminal.
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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private FileStatus status = FileStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "safety_class", length = 64)
    private FileSafetyClass safetyClass;

    @Enumerated(EnumType.STRING)
    @Column(name = "upload_context_key", length = 64)
    private UploadContextKey uploadContextKey;
}
