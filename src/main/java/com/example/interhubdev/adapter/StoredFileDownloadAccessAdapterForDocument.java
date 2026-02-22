package com.example.interhubdev.adapter;

import com.example.interhubdev.document.api.StoredFileDownloadAccessPort;
import com.example.interhubdev.submission.SubmissionApi;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adapter: implements Document module's StoredFileDownloadAccessPort using Submission module.
 * Allows teachers to download stored files that are attached to submissions for homeworks
 * of lessons they teach. Uses @Lazy on SubmissionApi to avoid circular dependency with Document.
 */
@Component
public class StoredFileDownloadAccessAdapterForDocument implements StoredFileDownloadAccessPort {

    private final SubmissionApi submissionApi;

    public StoredFileDownloadAccessAdapterForDocument(@Lazy SubmissionApi submissionApi) {
        this.submissionApi = submissionApi;
    }

    @Override
    public boolean canDownload(UUID storedFileId, UUID userId) {
        return submissionApi.canTeacherDownloadSubmissionFile(storedFileId, userId);
    }
}
