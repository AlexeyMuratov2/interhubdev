package com.example.interhubdev.fileasset;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Public API of the fileasset module.
 * <p>
 * Business modules choose a {@link FilePolicyKey} and interact with file lifecycle through this
 * interface. Storage details remain internal to the module.
 */
public interface FileAssetApi {

    FileAssetView register(
        FilePolicyKey policyKey,
        String originalName,
        String declaredContentType,
        long sizeBytes,
        UUID uploadedBy
    );

    FileAssetView markUploaded(UUID fileAssetId, FileUploadReceipt uploadReceipt);

    FileAssetView requestProcessing(UUID fileAssetId);

    FileAssetView confirmBound(UUID fileAssetId);

    Optional<FileAssetView> get(UUID fileAssetId);

    Map<UUID, FileAssetView> getMany(Set<UUID> fileAssetIds);

    FileAssetView markDeleted(UUID fileAssetId);
}
