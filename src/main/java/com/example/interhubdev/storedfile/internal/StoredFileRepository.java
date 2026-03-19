package com.example.interhubdev.storedfile.internal;

import com.example.interhubdev.storedfile.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repository for StoredFile entity.
 */
public interface StoredFileRepository extends JpaRepository<StoredFile, UUID> {
}
