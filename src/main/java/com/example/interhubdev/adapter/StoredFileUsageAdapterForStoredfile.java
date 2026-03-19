package com.example.interhubdev.adapter;

import com.example.interhubdev.document.DocumentStoredFileUsagePort;
import com.example.interhubdev.storedfile.StoredFileUsagePort;
import com.example.interhubdev.submission.SubmissionApi;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Implements storedfile's StoredFileUsagePort by aggregating document and submission.
 * Prevents storedfile from deleting files that are still referenced by document attachments or submissions.
 * Depends on {@link DocumentStoredFileUsagePort} (not DocumentApi) to avoid circular dependency with storedfile.
 */
@Component
public class StoredFileUsageAdapterForStoredfile implements StoredFileUsagePort {

    private final DocumentStoredFileUsagePort documentStoredFileUsagePort;
    private final SubmissionApi submissionApi;

    public StoredFileUsageAdapterForStoredfile(
        DocumentStoredFileUsagePort documentStoredFileUsagePort,
        @Lazy SubmissionApi submissionApi
    ) {
        this.documentStoredFileUsagePort = documentStoredFileUsagePort;
        this.submissionApi = submissionApi;
    }

    @Override
    public boolean isStoredFileInUse(UUID storedFileId) {
        return documentStoredFileUsagePort.isStoredFileInUse(storedFileId)
            || submissionApi.isStoredFileInUse(storedFileId);
    }
}
