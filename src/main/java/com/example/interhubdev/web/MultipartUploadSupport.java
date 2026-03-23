package com.example.interhubdev.web;

import com.example.interhubdev.error.Errors;
import com.example.interhubdev.fileasset.FileAssetUploadCommand;
import com.example.interhubdev.fileasset.FilePolicyKey;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Shared helper that turns multipart uploads into fileasset ingest commands.
 */
public final class MultipartUploadSupport {

    private MultipartUploadSupport() {
    }

    public static MultipartUploadBundle prepareMany(List<MultipartFile> files, UUID uploadedBy, FilePolicyKey policyKey) {
        if (files == null || files.isEmpty()) {
            return new MultipartUploadBundle(List.of(), List.of());
        }
        List<FileAssetUploadCommand> uploads = new ArrayList<>(files.size());
        List<Path> tempFiles = new ArrayList<>(files.size());
        try {
            for (MultipartFile file : files) {
                uploads.add(prepareOneInternal(file, uploadedBy, policyKey, tempFiles));
            }
            return new MultipartUploadBundle(List.copyOf(uploads), List.copyOf(tempFiles));
        } catch (RuntimeException runtimeException) {
            new MultipartUploadBundle(List.of(), tempFiles).close();
            throw runtimeException;
        }
    }

    public static MultipartUploadBundle prepareSingle(MultipartFile file, UUID uploadedBy, FilePolicyKey policyKey) {
        if (file == null) {
            return new MultipartUploadBundle(List.of(), List.of());
        }
        List<Path> tempFiles = new ArrayList<>(1);
        FileAssetUploadCommand upload = prepareOneInternal(file, uploadedBy, policyKey, tempFiles);
        return new MultipartUploadBundle(List.of(upload), List.copyOf(tempFiles));
    }

    private static FileAssetUploadCommand prepareOneInternal(
        MultipartFile file,
        UUID uploadedBy,
        FilePolicyKey policyKey,
        List<Path> tempFiles
    ) {
        if (file == null || file.isEmpty()) {
            throw Errors.badRequest("File is empty");
        }
        try {
            Path tempFile = Files.createTempFile("upload-", null);
            tempFiles.add(tempFile);
            file.transferTo(tempFile.toFile());
            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
            String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
            return new FileAssetUploadCommand(
                tempFile,
                originalName,
                contentType,
                file.getSize(),
                uploadedBy,
                policyKey
            );
        } catch (Exception exception) {
            throw Errors.of(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "MULTIPART_PREPARE_FAILED",
                "Failed to prepare uploaded file for processing.");
        }
    }
}
