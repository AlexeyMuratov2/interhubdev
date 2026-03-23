package com.example.interhubdev.submission.internal;

import com.example.interhubdev.submission.SubmissionAttachmentApi;
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
    private final SubmissionAttachmentApi submissionAttachmentApi;

    SubmissionsArchiveHandleImpl(String filename, ArchiveData data, UUID requesterId, SubmissionAttachmentApi submissionAttachmentApi) {
        this.filename = filename;
        this.data = data;
        this.requesterId = requesterId;
        this.submissionAttachmentApi = submissionAttachmentApi;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public void writeTo(OutputStream out) throws IOException {
        ZipArchiveWriter.write(out, data.info(), data.entries(), entry ->
            submissionAttachmentApi.download(entry.attachmentId(), requesterId).stream());
    }
}
