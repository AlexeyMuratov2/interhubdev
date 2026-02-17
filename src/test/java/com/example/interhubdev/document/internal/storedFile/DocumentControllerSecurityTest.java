package com.example.interhubdev.document.internal.storedFile;

import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.document.DocumentApi;
import com.example.interhubdev.document.internal.uploadSecurity.UploadSecurityErrors;
import com.example.interhubdev.user.Role;
import com.example.interhubdev.user.UserDto;
import com.example.interhubdev.user.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Security-focused tests for document upload: file too large, forbidden type,
 * suspicious filename, malware detected, antivirus unavailable. Verifies HTTP status
 * and error code in response.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("DocumentController upload security")
class DocumentControllerSecurityTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentApi documentApi;

    @MockitoBean
    private AuthApi authApi;

    private static UserDto authenticatedUser() {
        return new UserDto(
                USER_ID, "user@test.com", List.of(Role.STUDENT), UserStatus.ACTIVE,
                "Test", "User", null, null,
                LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("POST /api/documents/upload")
    class Upload {

        @Test
        @DisplayName("returns 413 and UPLOAD_FILE_TOO_LARGE when file exceeds max size")
        void fileTooLarge() throws Exception {
            when(authApi.getCurrentUser(any())).thenReturn(Optional.of(authenticatedUser()));
            when(documentApi.uploadFile(any(), any(), any(), anyLong(), any()))
                    .thenThrow(UploadSecurityErrors.fileTooLarge(52_428_800L));

            mockMvc.perform(MockMvcRequestBuilders.multipart("/api/documents/upload")
                            .file("file", "content".getBytes())
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isPayloadTooLarge())
                    .andExpect(jsonPath("$.code").value(UploadSecurityErrors.CODE_FILE_TOO_LARGE))
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("returns 400 and UPLOAD_MALWARE_DETECTED when antivirus finds malware")
        void malwareDetected() throws Exception {
            when(authApi.getCurrentUser(any())).thenReturn(Optional.of(authenticatedUser()));
            when(documentApi.uploadFile(any(), any(), any(), anyLong(), any()))
                    .thenThrow(UploadSecurityErrors.malwareDetected());

            mockMvc.perform(MockMvcRequestBuilders.multipart("/api/documents/upload")
                            .file("file", "content".getBytes())
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(UploadSecurityErrors.CODE_MALWARE_DETECTED))
                    .andExpect(jsonPath("$.message").value("File rejected"));
        }

        @Test
        @DisplayName("returns 503 and UPLOAD_AV_UNAVAILABLE when antivirus service unavailable")
        void antivirusUnavailable() throws Exception {
            when(authApi.getCurrentUser(any())).thenReturn(Optional.of(authenticatedUser()));
            when(documentApi.uploadFile(any(), any(), any(), anyLong(), any()))
                    .thenThrow(UploadSecurityErrors.avUnavailable("Antivirus service is temporarily unavailable. Please try again later."));

            mockMvc.perform(MockMvcRequestBuilders.multipart("/api/documents/upload")
                            .file("file", "content".getBytes())
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.code").value(UploadSecurityErrors.CODE_AV_UNAVAILABLE));
        }

        @Test
        @DisplayName("returns 400 and UPLOAD_FORBIDDEN_FILE_TYPE when content type not allowed")
        void forbiddenFileType() throws Exception {
            when(authApi.getCurrentUser(any())).thenReturn(Optional.of(authenticatedUser()));
            when(documentApi.uploadFile(any(), any(), any(), anyLong(), any()))
                    .thenThrow(UploadSecurityErrors.forbiddenFileType("Content type not allowed"));

            mockMvc.perform(MockMvcRequestBuilders.multipart("/api/documents/upload")
                            .file("file", "content".getBytes())
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(UploadSecurityErrors.CODE_FORBIDDEN_FILE_TYPE));
        }

        @Test
        @DisplayName("returns 400 and UPLOAD_EXTENSION_MISMATCH when extension does not match MIME")
        void extensionMismatch() throws Exception {
            when(authApi.getCurrentUser(any())).thenReturn(Optional.of(authenticatedUser()));
            when(documentApi.uploadFile(any(), any(), any(), anyLong(), any()))
                    .thenThrow(UploadSecurityErrors.extensionMismatch("File extension 'exe' does not match content type application/pdf"));

            mockMvc.perform(MockMvcRequestBuilders.multipart("/api/documents/upload")
                            .file("file", "content".getBytes())
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(UploadSecurityErrors.CODE_EXTENSION_MISMATCH));
        }

        @Test
        @DisplayName("returns 400 and UPLOAD_SUSPICIOUS_FILENAME when filename is malicious")
        void suspiciousFilename() throws Exception {
            when(authApi.getCurrentUser(any())).thenReturn(Optional.of(authenticatedUser()));
            when(documentApi.uploadFile(any(), any(), any(), anyLong(), any()))
                    .thenThrow(UploadSecurityErrors.suspiciousFilename("File name must not contain path separators or traversal"));

            mockMvc.perform(MockMvcRequestBuilders.multipart("/api/documents/upload")
                            .file("file", "content".getBytes())
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(UploadSecurityErrors.CODE_SUSPICIOUS_FILENAME));
        }

        @Test
        @DisplayName("returns 400 and UPLOAD_CONTENT_TYPE_MISMATCH when magic bytes do not match declared type")
        void contentTypeMismatch() throws Exception {
            when(authApi.getCurrentUser(any())).thenReturn(Optional.of(authenticatedUser()));
            when(documentApi.uploadFile(any(), any(), any(), anyLong(), any()))
                    .thenThrow(UploadSecurityErrors.contentTypeMismatch("File content does not match declared type"));

            mockMvc.perform(MockMvcRequestBuilders.multipart("/api/documents/upload")
                            .file("file", "content".getBytes())
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(UploadSecurityErrors.CODE_CONTENT_TYPE_MISMATCH));
        }

        @Test
        @DisplayName("returns 401 when not authenticated")
        void unauthenticated() throws Exception {
            when(authApi.getCurrentUser(any())).thenReturn(Optional.empty());

            mockMvc.perform(MockMvcRequestBuilders.multipart("/api/documents/upload")
                            .file("file", "content".getBytes())
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isUnauthorized());
        }
    }
}
