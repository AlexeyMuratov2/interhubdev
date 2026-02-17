package com.example.interhubdev.document.internal.storedFile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repository for StoredFile entity.
 */
public interface StoredFileRepository extends JpaRepository<StoredFile, UUID> {
}
