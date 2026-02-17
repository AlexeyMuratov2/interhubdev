package com.example.interhubdev.document;

/**
 * Result of file upload operation.
 * 
 * @param path path in storage where file was saved
 * @param size file size in bytes
 * @param contentType MIME type of the file
 */
public record UploadResult(
    String path,
    long size,
    String contentType
) {
}
