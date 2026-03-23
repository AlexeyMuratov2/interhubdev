package com.example.interhubdev.submission.internal;

import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.error.Errors;
import com.example.interhubdev.submission.SubmissionAttachmentApi;
import com.example.interhubdev.submission.SubmissionAttachmentDto;
import com.example.interhubdev.web.FilenameSanitizer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/submission-attachments")
@RequiredArgsConstructor
@Tag(name = "Submission Attachments", description = "Status lookup and download for submission attachments")
class SubmissionAttachmentController {

    private final SubmissionAttachmentApi submissionAttachmentApi;
    private final AuthApi authApi;

    @GetMapping("/{attachmentId}")
    @Operation(summary = "Get submission attachment status", description = "Returns safe metadata and processing status for a submission attachment.")
    public ResponseEntity<SubmissionAttachmentDto> get(
        @PathVariable UUID attachmentId,
        HttpServletRequest request
    ) {
        UUID requesterId = authApi.getCurrentUser(request)
            .map(user -> user.id())
            .orElseThrow(() -> Errors.unauthorized("Authentication required"));
        SubmissionAttachmentDto dto = submissionAttachmentApi.get(attachmentId, requesterId)
            .orElseThrow(() -> Errors.notFound("Submission attachment not found: " + attachmentId));
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{attachmentId}/download")
    @Operation(summary = "Download submission attachment", description = "Streams a submission attachment through backend-controlled delivery.")
    public ResponseEntity<InputStreamResource> download(
        @PathVariable UUID attachmentId,
        HttpServletRequest request
    ) {
        UUID requesterId = authApi.getCurrentUser(request)
            .map(user -> user.id())
            .orElseThrow(() -> Errors.unauthorized("Authentication required"));
        var handle = submissionAttachmentApi.download(attachmentId, requesterId);
        String safeFilename = FilenameSanitizer.sanitizeForContentDisposition(handle.originalName());
        String encodedFilename = URLEncoder.encode(safeFilename, StandardCharsets.UTF_8).replace("+", "%20");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename);
        headers.add("X-Content-Type-Options", "nosniff");
        return ResponseEntity.ok()
            .headers(headers)
            .body(new InputStreamResource(handle.stream()));
    }
}
