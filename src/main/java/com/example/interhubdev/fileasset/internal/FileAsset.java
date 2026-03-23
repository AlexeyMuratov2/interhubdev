package com.example.interhubdev.fileasset.internal;

import com.example.interhubdev.fileasset.FileAssetArchiveProfile;
import com.example.interhubdev.fileasset.FileAssetSafetyClass;
import com.example.interhubdev.fileasset.FileAssetStatus;
import com.example.interhubdev.fileasset.FileDeliveryProfile;
import com.example.interhubdev.fileasset.FilePolicyKey;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Canonical file lifecycle aggregate owned by the fileasset module.
 */
@Entity
@Table(
    name = "file_asset",
    indexes = {
        @Index(name = "idx_file_asset_status", columnList = "status"),
        @Index(name = "idx_file_asset_uploaded_by", columnList = "uploaded_by"),
        @Index(name = "idx_file_asset_policy_key", columnList = "policy_key"),
        @Index(name = "idx_file_asset_expires_at", columnList = "expires_at"),
        @Index(name = "idx_file_asset_created_at", columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileAsset {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_key", nullable = false, length = 64)
    private FilePolicyKey policyKey;

    @Column(name = "policy_version", nullable = false)
    private int policyVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private FileAssetStatus status;

    @Column(name = "original_name", nullable = false, length = 512)
    private String originalName;

    @Column(name = "declared_content_type", length = 255)
    private String declaredContentType;

    @Column(name = "detected_content_type", length = 255)
    private String detectedContentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "checksum", length = 255)
    private String checksum;

    @Column(name = "etag", length = 255)
    private String etag;

    @Column(name = "upload_receipt_token", length = 255)
    private String uploadReceiptToken;

    @Column(name = "uploaded_by")
    private UUID uploadedBy;

    @Column(name = "temp_object_key", length = 1024)
    private String tempObjectKey;

    @Column(name = "final_object_key", length = 1024)
    private String finalObjectKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "safety_class", length = 64)
    private FileAssetSafetyClass safetyClass;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_profile", length = 64)
    private FileDeliveryProfile deliveryProfile;

    @Enumerated(EnumType.STRING)
    @Column(name = "archive_profile", length = 64)
    private FileAssetArchiveProfile archiveProfile;

    @Column(name = "processing_attempts", nullable = false)
    @Builder.Default
    private int processingAttempts = 0;

    @Column(name = "last_failure_code", length = 128)
    private String lastFailureCode;

    @Column(name = "last_failure_message", length = 1000)
    private String lastFailureMessage;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
