package com.example.interhubdev.submission.internal;

import com.example.interhubdev.document.DocumentApi;
import com.example.interhubdev.submission.SubmissionsArchiveHandle;
import com.example.interhubdev.submission.internal.archive.ArchiveData;
import com.example.interhubdev.submission.internal.archive.ZipArchiveWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Implementation of {@link SubmissionsArchiveHandle} that writes ZIP to an output stream
 * using pre-loaded archive data and document API for file content.
 */
class SubmissionsArchiveHandleImpl implements SubmissionsArchiveHandle {

    private final String filename;
    private final ArchiveData data;
    private final UUID requesterId;
    private final DocumentApi documentApi;

    SubmissionsArchiveHandleImpl(String filename, ArchiveData data, UUID requesterId, DocumentApi documentApi) {
        this.filename = filename;
        this.data = data;
        this.requesterId = requesterId;
        this.documentApi = documentApi;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public void writeTo(OutputStream out) throws IOException {
        ZipArchiveWriter.write(out, data.info(), data.entries(), entry ->
            documentApi.downloadByStoredFileId(entry.storedFileId(), requesterId));
    }
}
