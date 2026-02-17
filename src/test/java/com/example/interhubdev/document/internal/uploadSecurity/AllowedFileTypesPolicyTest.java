package com.example.interhubdev.document.internal.uploadSecurity;

import com.example.interhubdev.error.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for allowed file types policy: forbidden MIME, missing content type,
 * extension mismatch with declared MIME.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AllowedFileTypesPolicy")
class AllowedFileTypesPolicyTest {

    private final AllowedFileTypesPolicy policy = new AllowedFileTypesPolicy();

    @Nested
    @DisplayName("Content type required")
    class ContentTypeRequired {

        @Test
        @DisplayName("rejects null content type")
        void nullContentType() {
            assertThatThrownBy(() -> policy.checkAllowed(null, "doc.pdf"))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getCode())
                            .isEqualTo(UploadSecurityErrors.CODE_FORBIDDEN_FILE_TYPE));
        }

        @Test
        @DisplayName("rejects blank content type")
        void blankContentType() {
            assertThatThrownBy(() -> policy.checkAllowed("   ", "doc.pdf"))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getCode())
                            .isEqualTo(UploadSecurityErrors.CODE_FORBIDDEN_FILE_TYPE));
        }
    }

    @Nested
    @DisplayName("Forbidden MIME types")
    class ForbiddenMime {

        @Test
        @DisplayName("rejects application/x-executable")
        void executable() {
            assertThatThrownBy(() -> policy.checkAllowed("application/x-executable", "app.exe"))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getCode())
                            .isEqualTo(UploadSecurityErrors.CODE_FORBIDDEN_FILE_TYPE));
        }

        @Test
        @DisplayName("rejects application/javascript")
        void javascript() {
            assertThatThrownBy(() -> policy.checkAllowed("application/javascript", "script.js"))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getCode())
                            .isEqualTo(UploadSecurityErrors.CODE_FORBIDDEN_FILE_TYPE));
        }

        @Test
        @DisplayName("rejects application/x-msdownload")
        void msdownload() {
            assertThatThrownBy(() -> policy.checkAllowed("application/x-msdownload", "setup.exe"))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getCode())
                            .isEqualTo(UploadSecurityErrors.CODE_FORBIDDEN_FILE_TYPE));
        }

        @Test
        @DisplayName("rejects text/html (XSS risk)")
        void html() {
            assertThatThrownBy(() -> policy.checkAllowed("text/html", "page.html"))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getCode())
                            .isEqualTo(UploadSecurityErrors.CODE_FORBIDDEN_FILE_TYPE));
        }
    }

    @Nested
    @DisplayName("Extension mismatch")
    class ExtensionMismatch {

        @Test
        @DisplayName("rejects .exe when declared type is application/pdf")
        void pdfMimeWithExeExtension() {
            assertThatThrownBy(() -> policy.checkAllowed("application/pdf", "document.exe"))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getCode())
                            .isEqualTo(UploadSecurityErrors.CODE_EXTENSION_MISMATCH));
        }

        @Test
        @DisplayName("rejects .pdf when declared type is image/png")
        void pngMimeWithPdfExtension() {
            assertThatThrownBy(() -> policy.checkAllowed("image/png", "image.pdf"))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getCode())
                            .isEqualTo(UploadSecurityErrors.CODE_EXTENSION_MISMATCH));
        }

        @Test
        @DisplayName("rejects .doc when declared type is application/vnd...docx")
        void docxMimeWithDocExtension() {
            assertThatThrownBy(() -> policy.checkAllowed(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "file.doc"))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getCode())
                            .isEqualTo(UploadSecurityErrors.CODE_EXTENSION_MISMATCH));
        }

        @Test
        @DisplayName("rejects .xls when declared type is xlsx")
        void xlsxMimeWithXlsExtension() {
            assertThatThrownBy(() -> policy.checkAllowed(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "data.xls"))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getCode())
                            .isEqualTo(UploadSecurityErrors.CODE_EXTENSION_MISMATCH));
        }
    }

    @Nested
    @DisplayName("Allowed types and extensions")
    class AllowedTypes {

        @Test
        @DisplayName("accepts PDF with .pdf extension")
        void pdf() {
            assertThatCode(() -> policy.checkAllowed("application/pdf", "document.pdf"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("accepts JPEG with .jpg extension")
        void jpg() {
            assertThatCode(() -> policy.checkAllowed("image/jpeg", "photo.jpg"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("accepts JPEG with .jpeg extension")
        void jpeg() {
            assertThatCode(() -> policy.checkAllowed("image/jpeg", "photo.jpeg"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("accepts Word docx")
        void docx() {
            assertThatCode(() -> policy.checkAllowed(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "report.docx"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("accepts Excel xlsx")
        void xlsx() {
            assertThatCode(() -> policy.checkAllowed(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "data.xlsx"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("accepts content type with charset parameter (normalized)")
        void contentTypeWithCharset() {
            assertThatCode(() -> policy.checkAllowed("text/plain; charset=utf-8", "readme.txt"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("accepts filename without extension (no extension check)")
        void noExtension() {
            assertThatCode(() -> policy.checkAllowed("application/pdf", "document"))
                    .doesNotThrowAnyException();
        }
    }
}
