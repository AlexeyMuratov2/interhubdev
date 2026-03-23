package com.example.interhubdev.document.internal.attachment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface DocumentAttachmentRepository extends JpaRepository<DocumentAttachment, UUID> {

    List<DocumentAttachment> findByOwnerTypeAndOwnerIdOrderBySortOrderAsc(DocumentAttachmentOwnerType ownerType, UUID ownerId);

    @Query("SELECT da FROM DocumentAttachment da WHERE da.ownerType = :ownerType AND da.ownerId IN :ownerIds ORDER BY da.ownerId, da.sortOrder")
    List<DocumentAttachment> findByOwnerTypeAndOwnerIdInOrderByOwnerIdAndSortOrder(
        @Param("ownerType") DocumentAttachmentOwnerType ownerType,
        @Param("ownerIds") Collection<UUID> ownerIds
    );

    Optional<DocumentAttachment> findByIdAndOwnerType(UUID id, DocumentAttachmentOwnerType ownerType);

    long countByFileAssetId(UUID fileAssetId);

    void deleteByOwnerTypeAndOwnerId(DocumentAttachmentOwnerType ownerType, UUID ownerId);
}
