package com.example.interhubdev.adapter;

import com.example.interhubdev.document.StoredFileUsagePort;
import com.example.interhubdev.submission.SubmissionApi;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adapter: implements Document module's StoredFileUsagePort using Submission module's SubmissionApi.
 * Prevents deletion of stored files that are attached to homework submissions.
 */
@Component
@RequiredArgsConstructor
public class StoredFileUsageAdapterForDocument implements StoredFileUsagePort {

    private final SubmissionApi submissionApi;

    @Override
    public boolean isStoredFileInUse(UUID storedFileId) {
        return submissionApi.isStoredFileInUse(storedFileId);
    }
}
