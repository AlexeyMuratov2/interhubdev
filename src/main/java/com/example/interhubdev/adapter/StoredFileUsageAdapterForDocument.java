package com.example.interhubdev.adapter;

import com.example.interhubdev.document.api.StoredFileUsagePort;
import com.example.interhubdev.submission.SubmissionApi;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adapter: implements Document module's StoredFileUsagePort using Submission module's SubmissionApi.
 * Prevents deletion of stored files that are attached to homework submissions.
 * Uses @Lazy on SubmissionApi to avoid circular dependency with Document (document → adapter → submission → … → document).
 */
@Component
public class StoredFileUsageAdapterForDocument implements StoredFileUsagePort {

    private final SubmissionApi submissionApi;

    public StoredFileUsageAdapterForDocument(@Lazy SubmissionApi submissionApi) {
        this.submissionApi = submissionApi;
    }

    @Override
    public boolean isStoredFileInUse(UUID storedFileId) {
        return submissionApi.isStoredFileInUse(storedFileId);
    }
}
