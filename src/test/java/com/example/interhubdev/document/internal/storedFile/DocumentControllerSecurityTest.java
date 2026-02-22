package com.example.interhubdev.document.internal.storedFile;

import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.document.DocumentApi;
import com.example.interhubdev.document.StoredFileDto;
import com.example.interhubdev.document.internal.storedFile.DocumentErrors;
import com.example.interhubdev.document.internal.uploadSecurity.UploadSecurityErrors;
import com.example.interhubdev.user.Role;
import com.example.interhubdev.user.UserDto;
import com.example.interhubdev.user.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
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
        @DisplayName("happy path: returns 201 and DTO with id/originalName/contentType/size; uploadFile called with correct multipart args")
        void uploadHappyPath() throws Exception {
            byte[] pdfContent = "%PDF-1.4 content".getBytes();
            UUID fileId = UUID.randomUUID();
            StoredFileDto dto = new StoredFileDto(
                    fileId,
                    pdfContent.length,
                    "application/pdf",
                    "a.pdf",
                    LocalDateTime.now(),
                    USER_ID
            );
            when(authApi.getCurrentUser(any())).thenReturn(Optional.of(authenticatedUser()));
            when(documentApi.uploadFile(any(), any(), any(), anyLong(), any())).thenReturn(dto);

            MockMultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", pdfContent);

            mockMvc.perform(MockMvcRequestBuilders.multipart("/api/documents/upload").file(file))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(fileId.toString()))
                    .andExpect(jsonPath("$.originalName").value("a.pdf"))
                    .andExpect(jsonPath("$.contentType").value("application/pdf"))
                    .andExpect(jsonPath("$.size").value(pdfContent.length));

            ArgumentCaptor<String> filenameCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> contentTypeCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Long> sizeCaptor = ArgumentCaptor.forClass(Long.class);
            verify(documentApi).uploadFile(any(), filenameCaptor.capture(), contentTypeCaptor.capture(), sizeCaptor.capture(), eq(USER_ID));
            assertThat(filenameCaptor.getValue()).isEqualTo("a.pdf");
            assertThat(contentTypeCaptor.getValue()).isEqualTo("application/pdf");
            assertThat(sizeCaptor.getValue()).isEqualTo((long) pdfContent.length);
        }

        @Test
        @DisplayName("when contentType missing in multipart, controller passes application/octet-stream to API")
        void contentTypeMissingUsesOctetStream() throws Exception {
            byte[] content = "data".getBytes();
            when(authApi.getCurrentUser(any())).thenReturn(Optional.of(authenticatedUser()));
            when(documentApi.uploadFile(any(), any(), any(), anyLong(), any()))
                    .thenThrow(UploadSecurityErrors.forbiddenFileType("Content type not allowed"));

            MockMultipartFile file = new MockMultipartFile("file", "a.pdf", null, content);

            mockMvc.perform(MockMvcRequestBuilders.multipart("/api/documents/upload").file(file))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(UploadSecurityErrors.CODE_FORBIDDEN_FILE_TYPE));

            ArgumentCaptor<String> contentTypeCaptor = ArgumentCaptor.forClass(String.class);
            verify(documentApi).uploadFile(any(), eq("a.pdf"), contentTypeCaptor.capture(), eq((long) content.length), eq(USER_ID));
            assertThat(contentTypeCaptor.getValue()).isEqualTo("application/octet-stream");
        }

        @Test
        @DisplayName("returns 400 when file content is empty (zero bytes)")
        void emptyFileContent() throws Exception {
            when(authApi.getCurrentUser(any())).thenReturn(Optional.of(authenticatedUser()));

            MockMultipartFile file = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

            mockMvc.perform(MockMvcRequestBuilders.multipart("/api/documents/upload").file(file))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("File is empty"));
        }

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

    @Nested
    @DisplayName("POST /api/documents/upload/batch")
    class UploadBatch {

        @Test
        @DisplayName("happy path: multiple files returns 201 and list of DTOs; uploadFiles called with correct inputs")
        void uploadBatchHappyPath() throws Exception {
            byte[] pdf1 = "%PDF-1.4 one".getBytes();
            byte[] pdf2 = "%PDF-1.4 two".getBytes();
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            StoredFileDto dto1 = new StoredFileDto(id1, pdf1.length, "application/pdf", "a.pdf", LocalDateTime.now(), USER_ID);
            StoredFileDto dto2 = new StoredFileDto(id2, pdf2.length, "application/pdf", "b.pdf", LocalDateTime.now(), USER_ID);
            when(authApi.getCurrentUser(any())).thenReturn(Optional.of(authenticatedUser()));
            when(documentApi.uploadFiles(any(), eq(USER_ID))).thenReturn(List.of(dto1, dto2));

            MockMultipartFile file1 = new MockMultipartFile("files", "a.pdf", "application/pdf", pdf1);
            MockMultipartFile file2 = new MockMultipartFile("files", "b.pdf", "application/pdf", pdf2);

            mockMvc.perform(MockMvcRequestBuilders.multipart("/api/documents/upload/batch").file(file1).file(file2))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].id").value(id1.toString()))
                    .andExpect(jsonPath("$[0].originalName").value("a.pdf"))
                    .andExpect(jsonPath("$[1].id").value(id2.toString()))
                    .andExpect(jsonPath("$[1].originalName").value("b.pdf"));

            verify(documentApi).uploadFiles(any(), eq(USER_ID));
        }

        @Test
        @DisplayName("returns 400 when one file fails security; uploadFiles throws and no partial result")
        void uploadBatchOneFileFailsSecurity() throws Exception {
            when(authApi.getCurrentUser(any())).thenReturn(Optional.of(authenticatedUser()));
            when(documentApi.uploadFiles(any(), eq(USER_ID)))
                    .thenThrow(UploadSecurityErrors.malwareDetected());

            MockMultipartFile file1 = new MockMultipartFile("files", "a.pdf", "application/pdf", "%PDF-1.4 a".getBytes());
            MockMultipartFile file2 = new MockMultipartFile("files", "b.pdf", "application/pdf", "%PDF-1.4 b".getBytes());

            mockMvc.perform(MockMvcRequestBuilders.multipart("/api/documents/upload/batch").file(file1).file(file2))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(UploadSecurityErrors.CODE_MALWARE_DETECTED));

            verify(documentApi).uploadFiles(any(), eq(USER_ID));
        }

        @Test
        @DisplayName("returns 400 when no files in request")
        void uploadBatchEmpty() throws Exception {
            when(authApi.getCurrentUser(any())).thenReturn(Optional.of(authenticatedUser()));

            mockMvc.perform(MockMvcRequestBuilders.multipart("/api/documents/upload/batch")
                            .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("At least one file is required"));
        }

        @Test
        @DisplayName("returns 400 BATCH_TOO_LARGE when too many files")
        void uploadBatchTooManyFiles() throws Exception {
            when(authApi.getCurrentUser(any())).thenReturn(Optional.of(authenticatedUser()));

            MockMultipartFile file = new MockMultipartFile("files", "a.pdf", "application/pdf", "content".getBytes());
            var request = MockMvcRequestBuilders.multipart("/api/documents/upload/batch");
            for (int i = 0; i < 51; i++) {
                request.file(file);
            }

            mockMvc.perform(request)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(DocumentErrors.CODE_BATCH_TOO_LARGE));
        }

        @Test
        @DisplayName("returns 401 when not authenticated")
        void unauthenticated() throws Exception {
            when(authApi.getCurrentUser(any())).thenReturn(Optional.empty());

            MockMultipartFile file = new MockMultipartFile("files", "a.pdf", "application/pdf", "content".getBytes());
            mockMvc.perform(MockMvcRequestBuilders.multipart("/api/documents/upload/batch").file(file))
                    .andExpect(status().isUnauthorized());
        }
    }
}
