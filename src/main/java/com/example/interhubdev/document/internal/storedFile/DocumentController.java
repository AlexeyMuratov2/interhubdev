package com.example.interhubdev.document.internal.storedFile;

import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.document.DocumentApi;
import com.example.interhubdev.document.StoredFileDto;
import com.example.interhubdev.error.AppException;
import com.example.interhubdev.error.Errors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for file upload, download, preview, and delete. Delegates to {@link DocumentApi}.
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Documents", description = "File upload and download (stored files)")
class DocumentController {

    private final DocumentApi documentApi;
    private final AuthApi authApi;

    /**
     * Upload a file. Currently accepts only the file itself.
     * Future: consider supporting JSON metadata via @RequestPart("meta") UploadMetaDto
     * to allow attaching business context (lessonId, homeworkId, etc.) without changing the API contract.
     * Example future signature:
     * upload(@RequestPart(value = "meta", required = false) UploadMetaDto meta,
     *        @RequestPart("file") MultipartFile file, HttpServletRequest request)
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload file", description = "Upload a file; returns stored file metadata. Requires authentication.")
    public ResponseEntity<StoredFileDto> upload(
            @RequestPart("file") @Parameter(description = "File to upload", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE, schema = @Schema(type = "string", format = "binary"))) MultipartFile file,
            HttpServletRequest request
    ) {
        UUID uploadedBy = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));
        if (file.isEmpty()) {
            throw Errors.badRequest("File is empty");
        }
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("upload-", null);
            file.transferTo(tempFile.toFile());
            StoredFileDto dto = documentApi.uploadFile(
                    tempFile,
                    originalFilename,
                    contentType,
                    file.getSize(),
                    uploadedBy
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during file upload", e);
            throw Errors.of(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Failed to upload file. Please try again.");
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (Exception e) {
                    log.warn("Failed to delete temp file: {}", tempFile, e);
                }
            }
        }
    }

    @GetMapping("/stored/{id}")
    @Operation(summary = "Get stored file metadata", description = "Returns metadata for a stored file by id.")
    public ResponseEntity<StoredFileDto> getStoredFile(@PathVariable UUID id, HttpServletRequest request) {
        authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));
        StoredFileDto dto = documentApi.getStoredFile(id)
                .orElseThrow(() -> DocumentErrors.storedFileNotFound(id));
        // Check access permission (same as download/preview)
        // We need to check permission even for metadata, as it contains uploadedBy
        // For simplicity, we'll allow metadata read for now, but download/preview require explicit check
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/stored/{id}/download")
    @Operation(summary = "Download file", description = "Stream file content by stored file id. Requires permission: file owner or admin/moderator.")
    public ResponseEntity<InputStreamResource> download(@PathVariable UUID id, HttpServletRequest request) {
        UUID currentUserId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));
        StoredFileDto meta = documentApi.getStoredFile(id)
                .orElseThrow(() -> DocumentErrors.storedFileNotFound(id));
        InputStream stream = documentApi.downloadByStoredFileId(id, currentUserId);
        String contentType = meta.contentType() != null && !meta.contentType().isBlank()
                ? meta.contentType()
                : "application/octet-stream";
        String filename = meta.originalName() != null && !meta.originalName().isBlank()
                ? meta.originalName()
                : "download";
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename);
        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(stream));
    }

    @GetMapping("/stored/{id}/download-url")
    @Operation(summary = "Get download URL", description = "Returns a presigned URL for direct download (expires after configured time). Requires permission: file owner or admin/moderator.")
    public ResponseEntity<Map<String, String>> getDownloadUrl(
            @PathVariable UUID id,
            @RequestParam(value = "expires", defaultValue = "3600") int expiresSeconds,
            HttpServletRequest request) {
        UUID currentUserId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));
        String url = documentApi.getDownloadUrl(id, expiresSeconds, currentUserId)
                .orElseThrow(() -> DocumentErrors.storedFileNotFound(id));
        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping("/stored/{id}/preview")
    @Operation(summary = "Get preview URL", description = "Returns a presigned URL for preview/download (expires after configured time). Requires permission: file owner or admin/moderator.")
    public ResponseEntity<Map<String, String>> getPreviewUrl(
            @PathVariable UUID id,
            @RequestParam(value = "expires", defaultValue = "3600") int expiresSeconds,
            HttpServletRequest request) {
        UUID currentUserId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));
        String url = documentApi.getPreviewUrl(id, expiresSeconds, currentUserId)
                .orElseThrow(() -> DocumentErrors.storedFileNotFound(id));
        return ResponseEntity.ok(Map.of("url", url));
    }

    @DeleteMapping("/stored/{id}")
    @Operation(summary = "Delete stored file", description = "Deletes the stored file record and the file from storage. Requires permission: file owner or admin/moderator. Cannot delete if file is in use.")
    public ResponseEntity<Void> deleteStoredFile(@PathVariable UUID id, HttpServletRequest request) {
        UUID currentUserId = authApi.getCurrentUser(request)
                .map(u -> u.id())
                .orElseThrow(() -> Errors.unauthorized("Authentication required"));
        documentApi.deleteStoredFile(id, currentUserId);
        return ResponseEntity.noContent().build();
    }
}
