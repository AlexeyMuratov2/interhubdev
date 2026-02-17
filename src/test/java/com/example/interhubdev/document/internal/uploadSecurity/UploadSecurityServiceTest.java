package com.example.interhubdev.document.internal.uploadSecurity;

import com.example.interhubdev.document.UploadContext;
import com.example.interhubdev.error.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for upload security: file size, allowed types, extension mismatch,
 * magic bytes vs declared type, antivirus (INFECTED/ERROR), malicious filenames.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UploadSecurityService security")
class UploadSecurityServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final long MAX_FILE_SIZE = 1024L; // 1 KB for tests

    @Mock
    private MaliciousFileChecks maliciousFileChecks;
    @Mock
    private AllowedFileTypesPolicy allowedFileTypesPolicy;
    @Mock
    private ContentSniffer contentSniffer;
    @Mock
    private AntivirusPort antivirusPort;

    @InjectMocks
    private UploadSecurityService uploadSecurityService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(uploadSecurityService, "maxFileSizeBytes", MAX_FILE_SIZE);
    }

    @Nested
    @DisplayName("File size")
    class FileSize {

        @Test
        @DisplayName("rejects zero size with UPLOAD_EMPTY_FILE")
        void zeroSize() {
            UploadContext ctx = UploadContext.of(USER_ID, "application/pdf", 0, "doc.pdf");

            assertThatThrownBy(() -> uploadSecurityService.ensureUploadAllowed(ctx, null))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> {
                        AppException e = (AppException) ex;
                        assertThat(e.getCode()).isEqualTo(UploadSecurityErrors.CODE_EMPTY_FILE);
                        assertThat(e.getStatus()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
                    });
            verify(maliciousFileChecks, never()).checkFilename(any());
        }

        @Test
        @DisplayName("rejects negative size with UPLOAD_EMPTY_FILE")
        void negativeSize() {
            UploadContext ctx = UploadContext.of(USER_ID, "application/pdf", -1, "doc.pdf");

            assertThatThrownBy(() -> uploadSecurityService.ensureUploadAllowed(ctx, null))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getCode())
                            .isEqualTo(UploadSecurityErrors.CODE_EMPTY_FILE));
        }

        @Test
        @DisplayName("rejects file exceeding max size")
        void fileTooLarge() {
            UploadContext ctx = UploadContext.of(USER_ID, "application/pdf", MAX_FILE_SIZE + 1, "doc.pdf");

            assertThatThrownBy(() -> uploadSecurityService.ensureUploadAllowed(ctx, null))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> {
                        AppException e = (AppException) ex;
                        assertThat(e.getCode()).isEqualTo(UploadSecurityErrors.CODE_FILE_TOO_LARGE);
                        assertThat(e.getStatus()).isEqualTo(org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE);
                    });
        }

        @Test
        @DisplayName("accepts file at exactly max size")
        void atMaxSize() {
            UploadContext ctx = UploadContext.of(USER_ID, "application/pdf", MAX_FILE_SIZE, "doc.pdf");
            doNothing().when(maliciousFileChecks).checkFilename("doc.pdf");
            doNothing().when(allowedFileTypesPolicy).checkAllowed("application/pdf", "doc.pdf");

            assertThatCode(() -> uploadSecurityService.ensureUploadAllowed(ctx, null))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Allowed types and extension")
    class AllowedTypesAndExtension {

        @Test
        @DisplayName("rejects forbidden content type via policy")
        void forbiddenContentType() {
            UploadContext ctx = UploadContext.of(USER_ID, "application/x-executable", 100, "virus.exe");
            doNothing().when(maliciousFileChecks).checkFilename(any());
            doThrow(UploadSecurityErrors.forbiddenFileType("Content type not allowed"))
                    .when(allowedFileTypesPolicy).checkAllowed(eq("application/x-executable"), eq("virus.exe"));

            assertThatThrownBy(() -> uploadSecurityService.ensureUploadAllowed(ctx, null))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getCode())
                            .isEqualTo(UploadSecurityErrors.CODE_FORBIDDEN_FILE_TYPE));
        }

        @Test
        @DisplayName("rejects extension mismatch (e.g. .exe with PDF MIME)")
        void extensionMismatch() {
            UploadContext ctx = UploadContext.of(USER_ID, "application/pdf", 100, "doc.exe");
            doNothing().when(maliciousFileChecks).checkFilename(any());
            doThrow(UploadSecurityErrors.extensionMismatch("File extension 'exe' does not match content type application/pdf"))
                    .when(allowedFileTypesPolicy).checkAllowed(eq("application/pdf"), eq("doc.exe"));

            assertThatThrownBy(() -> uploadSecurityService.ensureUploadAllowed(ctx, null))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getCode())
                            .isEqualTo(UploadSecurityErrors.CODE_EXTENSION_MISMATCH));
        }
    }

    @Nested
    @DisplayName("Malicious filename")
    class MaliciousFilename {

        @Test
        @DisplayName("rejects path traversal in filename")
        void pathTraversal() {
            UploadContext ctx = UploadContext.of(USER_ID, "application/pdf", 100, "../../../etc/passwd");
            doThrow(UploadSecurityErrors.suspiciousFilename("File name must not contain path separators or traversal"))
                    .when(maliciousFileChecks).checkFilename("../../../etc/passwd");

            assertThatThrownBy(() -> uploadSecurityService.ensureUploadAllowed(ctx, null))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getCode())
                            .isEqualTo(UploadSecurityErrors.CODE_SUSPICIOUS_FILENAME));
        }

        @Test
        @DisplayName("rejects double extension masking")
        void doubleExtension() {
            UploadContext ctx = UploadContext.of(USER_ID, "application/pdf", 100, "doc.pdf.exe");
            doThrow(UploadSecurityErrors.suspiciousFilename("Executable or script file types are not allowed"))
                    .when(maliciousFileChecks).checkFilename("doc.pdf.exe");

            assertThatThrownBy(() -> uploadSecurityService.ensureUploadAllowed(ctx, null))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getCode())
                            .isEqualTo(UploadSecurityErrors.CODE_SUSPICIOUS_FILENAME));
        }
    }

    @Nested
    @DisplayName("Content type vs magic bytes")
    class ContentTypeMismatch {

        @Test
        @DisplayName("rejects when magic bytes do not match declared MIME")
        void contentMismatch() throws Exception {
            Path tempFile = java.nio.file.Files.createTempFile("upload-", ".bin");
            try {
                java.nio.file.Files.write(tempFile, new byte[]{0x50, 0x4B, 0x03, 0x04}); // ZIP magic
                UploadContext ctx = UploadContext.of(USER_ID, "application/pdf", 4, "doc.pdf");
                doNothing().when(maliciousFileChecks).checkFilename(any());
                doNothing().when(allowedFileTypesPolicy).checkAllowed(any(), any());
                when(contentSniffer.detectMimeFromContent(tempFile)).thenReturn(Optional.of("application/zip"));

                assertThatThrownBy(() -> uploadSecurityService.ensureUploadAllowed(ctx, tempFile))
                        .isInstanceOf(AppException.class)
                        .satisfies(ex -> {
                            AppException e = (AppException) ex;
                            assertThat(e.getCode()).isEqualTo(UploadSecurityErrors.CODE_CONTENT_TYPE_MISMATCH);
                            assertThat(e.getStatus()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
                        });
            } finally {
                java.nio.file.Files.deleteIfExists(tempFile);
            }
        }

        @Test
        @DisplayName("accepts when magic bytes match declared MIME")
        void contentMatch() throws Exception {
            Path tempFile = java.nio.file.Files.createTempFile("upload-", ".pdf");
            try {
                java.nio.file.Files.write(tempFile, "%PDF-1.4".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
                UploadContext ctx = UploadContext.of(USER_ID, "application/pdf", 8, "doc.pdf");
                doNothing().when(maliciousFileChecks).checkFilename(any());
                doNothing().when(allowedFileTypesPolicy).checkAllowed(any(), any());
                when(contentSniffer.detectMimeFromContent(tempFile)).thenReturn(Optional.of("application/pdf"));
                when(antivirusPort.scan(any(), any(), any())).thenReturn(AntivirusPort.ScanResult.clean());

                assertThatCode(() -> uploadSecurityService.ensureUploadAllowed(ctx, tempFile))
                        .doesNotThrowAnyException();
            } finally {
                java.nio.file.Files.deleteIfExists(tempFile);
            }
        }
    }

    @Nested
    @DisplayName("Antivirus")
    class Antivirus {

        @Test
        @DisplayName("rejects when antivirus returns INFECTED (FOUND)")
        void antivirusInfected() throws Exception {
            Path tempFile = java.nio.file.Files.createTempFile("upload-", ".bin");
            try {
                java.nio.file.Files.write(tempFile, new byte[]{'%', 'P', 'D', 'F'});
                UploadContext ctx = UploadContext.of(USER_ID, "application/pdf", 4, "doc.pdf");
                doNothing().when(maliciousFileChecks).checkFilename(any());
                doNothing().when(allowedFileTypesPolicy).checkAllowed(any(), any());
                when(contentSniffer.detectMimeFromContent(tempFile)).thenReturn(Optional.of("application/pdf"));
                when(antivirusPort.scan(eq(tempFile), eq("doc.pdf"), eq("application/pdf")))
                        .thenReturn(AntivirusPort.ScanResult.infected("Eicar-Test-Signature"));

                assertThatThrownBy(() -> uploadSecurityService.ensureUploadAllowed(ctx, tempFile))
                        .isInstanceOf(AppException.class)
                        .satisfies(ex -> {
                            AppException e = (AppException) ex;
                            assertThat(e.getCode()).isEqualTo(UploadSecurityErrors.CODE_MALWARE_DETECTED);
                            assertThat(e.getStatus()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
                            assertThat(e.getMessage()).doesNotContain("Eicar"); // signature must not leak
                        });
            } finally {
                java.nio.file.Files.deleteIfExists(tempFile);
            }
        }

        @Test
        @DisplayName("rejects when antivirus returns ERROR (fail-closed)")
        void antivirusError() throws Exception {
            Path tempFile = java.nio.file.Files.createTempFile("upload-", ".bin");
            try {
                java.nio.file.Files.write(tempFile, new byte[]{'%', 'P', 'D', 'F'});
                UploadContext ctx = UploadContext.of(USER_ID, "application/pdf", 4, "doc.pdf");
                doNothing().when(maliciousFileChecks).checkFilename(any());
                doNothing().when(allowedFileTypesPolicy).checkAllowed(any(), any());
                when(contentSniffer.detectMimeFromContent(tempFile)).thenReturn(Optional.of("application/pdf"));
                when(antivirusPort.scan(any(), any(), any())).thenReturn(AntivirusPort.ScanResult.error());

                assertThatThrownBy(() -> uploadSecurityService.ensureUploadAllowed(ctx, tempFile))
                        .isInstanceOf(AppException.class)
                        .satisfies(ex -> {
                            AppException e = (AppException) ex;
                            assertThat(e.getCode()).isEqualTo(UploadSecurityErrors.CODE_AV_UNAVAILABLE);
                            assertThat(e.getStatus()).isEqualTo(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE);
                        });
            } finally {
                java.nio.file.Files.deleteIfExists(tempFile);
            }
        }

        @Test
        @DisplayName("accepts when antivirus returns CLEAN")
        void antivirusClean() throws Exception {
            Path tempFile = java.nio.file.Files.createTempFile("upload-", ".pdf");
            try {
                java.nio.file.Files.write(tempFile, "%PDF-1.4".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
                UploadContext ctx = UploadContext.of(USER_ID, "application/pdf", 8, "doc.pdf");
                doNothing().when(maliciousFileChecks).checkFilename(any());
                doNothing().when(allowedFileTypesPolicy).checkAllowed(any(), any());
                when(contentSniffer.detectMimeFromContent(tempFile)).thenReturn(Optional.of("application/pdf"));
                when(antivirusPort.scan(any(), any(), any())).thenReturn(AntivirusPort.ScanResult.clean());

                assertThatCode(() -> uploadSecurityService.ensureUploadAllowed(ctx, tempFile))
                        .doesNotThrowAnyException();
            } finally {
                java.nio.file.Files.deleteIfExists(tempFile);
            }
        }
    }

    @Nested
    @DisplayName("Metadata-only (no content path)")
    class MetadataOnly {

        @Test
        @DisplayName("does not call content sniffer or antivirus when contentPath is null")
        void skipsContentChecks() {
            UploadContext ctx = UploadContext.of(USER_ID, "application/pdf", 100, "doc.pdf");
            doNothing().when(maliciousFileChecks).checkFilename(any());
            doNothing().when(allowedFileTypesPolicy).checkAllowed(any(), any());

            assertThatCode(() -> uploadSecurityService.ensureUploadAllowed(ctx, null))
                    .doesNotThrowAnyException();

            verify(contentSniffer, never()).detectMimeFromContent(any());
            verify(antivirusPort, never()).scan(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Call order and short-circuit")
    class CallOrderAndShortCircuit {

        @Test
        @DisplayName("when content path provided, calls maliciousFileChecks then allowedFileTypes then sniffer then antivirus")
        void orderOfCallsWhenContentPresent() throws Exception {
            Path tempFile = java.nio.file.Files.createTempFile("upload-", ".pdf");
            try {
                java.nio.file.Files.write(tempFile, "%PDF-1.4".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
                UploadContext ctx = UploadContext.of(USER_ID, "application/pdf", 8, "doc.pdf");
                doNothing().when(maliciousFileChecks).checkFilename(any());
                doNothing().when(allowedFileTypesPolicy).checkAllowed(any(), any());
                when(contentSniffer.detectMimeFromContent(tempFile)).thenReturn(Optional.of("application/pdf"));
                when(antivirusPort.scan(any(), any(), any())).thenReturn(AntivirusPort.ScanResult.clean());

                uploadSecurityService.ensureUploadAllowed(ctx, tempFile);

                var inOrder = org.mockito.Mockito.inOrder(maliciousFileChecks, allowedFileTypesPolicy, contentSniffer, antivirusPort);
                inOrder.verify(maliciousFileChecks).checkFilename("doc.pdf");
                inOrder.verify(allowedFileTypesPolicy).checkAllowed("application/pdf", "doc.pdf");
                inOrder.verify(contentSniffer).detectMimeFromContent(tempFile);
                inOrder.verify(antivirusPort).scan(eq(tempFile), eq("doc.pdf"), eq("application/pdf"));
            } finally {
                java.nio.file.Files.deleteIfExists(tempFile);
            }
        }

        @Test
        @DisplayName("when filename is suspicious, allowed types / sniffer / antivirus are not called")
        void suspiciousFilenameShortCircuit() {
            UploadContext ctx = UploadContext.of(USER_ID, "application/pdf", 100, "../../etc/passwd");
            doThrow(UploadSecurityErrors.suspiciousFilename("path traversal"))
                    .when(maliciousFileChecks).checkFilename("../../etc/passwd");

            assertThatThrownBy(() -> uploadSecurityService.ensureUploadAllowed(ctx, null))
                    .isInstanceOf(AppException.class);

            verify(allowedFileTypesPolicy, never()).checkAllowed(any(), any());
            verify(contentSniffer, never()).detectMimeFromContent(any());
            verify(antivirusPort, never()).scan(any(), any(), any());
        }

        @Test
        @DisplayName("when MIME forbidden, sniffer and antivirus are not called")
        void forbiddenMimeShortCircuit() {
            UploadContext ctx = UploadContext.of(USER_ID, "application/x-executable", 100, "virus.exe");
            doNothing().when(maliciousFileChecks).checkFilename(any());
            doThrow(UploadSecurityErrors.forbiddenFileType("not allowed"))
                    .when(allowedFileTypesPolicy).checkAllowed(eq("application/x-executable"), eq("virus.exe"));

            assertThatThrownBy(() -> uploadSecurityService.ensureUploadAllowed(ctx, null))
                    .isInstanceOf(AppException.class);

            verify(contentSniffer, never()).detectMimeFromContent(any());
            verify(antivirusPort, never()).scan(any(), any(), any());
        }

        @Test
        @DisplayName("when sniffing mismatch, antivirus is not called")
        void sniffingMismatchShortCircuit() throws Exception {
            Path tempFile = java.nio.file.Files.createTempFile("upload-", ".bin");
            try {
                java.nio.file.Files.write(tempFile, new byte[]{0x50, 0x4B, 0x03, 0x04});
                UploadContext ctx = UploadContext.of(USER_ID, "application/pdf", 4, "doc.pdf");
                doNothing().when(maliciousFileChecks).checkFilename(any());
                doNothing().when(allowedFileTypesPolicy).checkAllowed(any(), any());
                when(contentSniffer.detectMimeFromContent(tempFile)).thenReturn(Optional.of("application/zip"));

                assertThatThrownBy(() -> uploadSecurityService.ensureUploadAllowed(ctx, tempFile))
                        .isInstanceOf(AppException.class)
                        .satisfies(ex -> assertThat(((AppException) ex).getCode())
                                .isEqualTo(UploadSecurityErrors.CODE_CONTENT_TYPE_MISMATCH));

                verify(antivirusPort, never()).scan(any(), any(), any());
            } finally {
                java.nio.file.Files.deleteIfExists(tempFile);
            }
        }
    }

    @Nested
    @DisplayName("Antivirus exception")
    class AntivirusException {

        @Test
        @DisplayName("when antivirus throws (e.g. connect refused), maps to AV_UNAVAILABLE")
        void antivirusThrowsMapsToUnavailable() throws Exception {
            Path tempFile = java.nio.file.Files.createTempFile("upload-", ".pdf");
            try {
                java.nio.file.Files.write(tempFile, "%PDF-1.4".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
                UploadContext ctx = UploadContext.of(USER_ID, "application/pdf", 8, "doc.pdf");
                doNothing().when(maliciousFileChecks).checkFilename(any());
                doNothing().when(allowedFileTypesPolicy).checkAllowed(any(), any());
                when(contentSniffer.detectMimeFromContent(tempFile)).thenReturn(Optional.of("application/pdf"));
                when(antivirusPort.scan(any(), any(), any()))
                        .thenThrow(new RuntimeException("Connection refused"));

                assertThatThrownBy(() -> uploadSecurityService.ensureUploadAllowed(ctx, tempFile))
                        .isInstanceOf(AppException.class)
                        .satisfies(ex -> {
                            AppException e = (AppException) ex;
                            assertThat(e.getCode()).isEqualTo(UploadSecurityErrors.CODE_AV_UNAVAILABLE);
                            assertThat(e.getStatus()).isEqualTo(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE);
                        });
            } finally {
                java.nio.file.Files.deleteIfExists(tempFile);
            }
        }
    }
}
