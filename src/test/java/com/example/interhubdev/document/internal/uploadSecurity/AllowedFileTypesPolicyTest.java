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

        @Test
        @DisplayName("rejects application/x-sh")
        void shellScript() {
            assertThatThrownBy(() -> policy.checkAllowed("application/x-sh", "script.sh"))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getCode())
                            .isEqualTo(UploadSecurityErrors.CODE_FORBIDDEN_FILE_TYPE));
        }
    }

    @Nested
    @DisplayName("Forbidden extensions")
    class ForbiddenExtensions {

        @Test
        @DisplayName("rejects .exe extension")
        void exe() {
            assertThatThrownBy(() -> policy.checkAllowed("application/pdf", "malware.exe"))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getCode())
                            .isEqualTo(UploadSecurityErrors.CODE_FORBIDDEN_FILE_TYPE));
        }

        @Test
        @DisplayName("rejects .bat extension")
        void bat() {
            assertThatThrownBy(() -> policy.checkAllowed("text/plain", "script.bat"))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getCode())
                            .isEqualTo(UploadSecurityErrors.CODE_FORBIDDEN_FILE_TYPE));
        }

        @Test
        @DisplayName("rejects .sh extension")
        void sh() {
            assertThatThrownBy(() -> policy.checkAllowed("text/plain", "script.sh"))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getCode())
                            .isEqualTo(UploadSecurityErrors.CODE_FORBIDDEN_FILE_TYPE));
        }

        @Test
        @DisplayName("rejects .jar extension")
        void jar() {
            assertThatThrownBy(() -> policy.checkAllowed("application/zip", "app.jar"))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getCode())
                            .isEqualTo(UploadSecurityErrors.CODE_FORBIDDEN_FILE_TYPE));
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
        @DisplayName("accepts PNG")
        void png() {
            assertThatCode(() -> policy.checkAllowed("image/png", "image.png"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("accepts GIF")
        void gif() {
            assertThatCode(() -> policy.checkAllowed("image/gif", "animation.gif"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("accepts WebP")
        void webp() {
            assertThatCode(() -> policy.checkAllowed("image/webp", "image.webp"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("accepts SVG")
        void svg() {
            assertThatCode(() -> policy.checkAllowed("image/svg+xml", "diagram.svg"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("accepts MP4 video")
        void mp4() {
            assertThatCode(() -> policy.checkAllowed("video/mp4", "video.mp4"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("accepts MP3 audio")
        void mp3() {
            assertThatCode(() -> policy.checkAllowed("audio/mpeg", "audio.mp3"))
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
        @DisplayName("accepts PowerPoint pptx")
        void pptx() {
            assertThatCode(() -> policy.checkAllowed(
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation", "presentation.pptx"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("accepts ZIP archive")
        void zip() {
            assertThatCode(() -> policy.checkAllowed("application/zip", "archive.zip"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("accepts content type with charset parameter (normalized)")
        void contentTypeWithCharset() {
            assertThatCode(() -> policy.checkAllowed("text/plain; charset=utf-8", "readme.txt"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("accepts application/pdf with parameters (e.g. charset=binary)")
        void contentTypeWithParamsNonText() {
            assertThatCode(() -> policy.checkAllowed("application/pdf; charset=binary", "a.pdf"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("accepts image/jpeg with extra parameters")
        void contentTypeWithParamsImage() {
            assertThatCode(() -> policy.checkAllowed("image/jpeg; something=value", "photo.jpg"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("accepts filename without extension")
        void noExtension() {
            assertThatCode(() -> policy.checkAllowed("application/pdf", "document"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("accepts different image extension with image MIME")
        void imageExtensionFlexibility() {
            // Now we allow flexibility - any safe extension with safe MIME
            assertThatCode(() -> policy.checkAllowed("image/png", "image.custom"))
                    .doesNotThrowAnyException();
        }
    }
}
