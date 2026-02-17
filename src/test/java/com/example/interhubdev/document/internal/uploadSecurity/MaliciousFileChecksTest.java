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
 * Unit tests for malicious filename checks: path traversal, null byte,
 * double/masked extensions, reserved names, dangerous extensions, length, trailing dot/space.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MaliciousFileChecks")
class MaliciousFileChecksTest {

    private final MaliciousFileChecks maliciousFileChecks = new MaliciousFileChecks();

    @Nested
    @DisplayName("Filename required")
    class FilenameRequired {

        @Test
        @DisplayName("rejects null filename")
        void nullFilename() {
            assertThatThrownBy(() -> maliciousFileChecks.checkFilename(null))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getCode())
                            .isEqualTo(UploadSecurityErrors.CODE_SUSPICIOUS_FILENAME));
        }

        @Test
        @DisplayName("rejects blank filename")
        void blankFilename() {
            assertThatThrownBy(() -> maliciousFileChecks.checkFilename("   "))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getCode())
                            .isEqualTo(UploadSecurityErrors.CODE_SUSPICIOUS_FILENAME));
        }
    }

    @Nested
    @DisplayName("Path traversal and separators")
    class PathTraversal {

        @Test
        @DisplayName("rejects path traversal (../)")
        void pathTraversalBackslash() {
            assertThatThrownBy(() -> maliciousFileChecks.checkFilename("..\\..\\etc\\passwd"))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getCode())
                            .isEqualTo(UploadSecurityErrors.CODE_SUSPICIOUS_FILENAME));
        }

        @Test
        @DisplayName("rejects path traversal (../) with slash")
        void pathTraversalSlash() {
            assertThatThrownBy(() -> maliciousFileChecks.checkFilename("../../../etc/passwd"))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getCode())
                            .isEqualTo(UploadSecurityErrors.CODE_SUSPICIOUS_FILENAME));
        }

        @Test
        @DisplayName("rejects absolute path")
        void absolutePath() {
            assertThatThrownBy(() -> maliciousFileChecks.checkFilename("/etc/passwd"))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getCode())
                            .isEqualTo(UploadSecurityErrors.CODE_SUSPICIOUS_FILENAME));
        }
    }

    @Nested
    @DisplayName("Null byte and control characters")
    class NullByteAndControlChars {

        @Test
        @DisplayName("rejects null byte in filename")
        void nullByte() {
            assertThatThrownBy(() -> maliciousFileChecks.checkFilename("doc\u0000.pdf"))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getCode())
                            .isEqualTo(UploadSecurityErrors.CODE_SUSPICIOUS_FILENAME));
        }

        @Test
        @DisplayName("rejects control characters in filename")
        void controlChars() {
            assertThatThrownBy(() -> maliciousFileChecks.checkFilename("doc\u0001.pdf"))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getCode())
                            .isEqualTo(UploadSecurityErrors.CODE_SUSPICIOUS_FILENAME));
        }
    }

    @Nested
    @DisplayName("Trailing dot and whitespace")
    class TrailingDotAndWhitespace {

        @Test
        @DisplayName("rejects filename ending with dot")
        void trailingDot() {
            assertThatThrownBy(() -> maliciousFileChecks.checkFilename("doc.pdf."))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getCode())
                            .isEqualTo(UploadSecurityErrors.CODE_SUSPICIOUS_FILENAME));
        }

        @Test
        @DisplayName("rejects filename ending with space")
        void trailingSpace() {
            assertThatThrownBy(() -> maliciousFileChecks.checkFilename("doc.pdf "))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getCode())
                            .isEqualTo(UploadSecurityErrors.CODE_SUSPICIOUS_FILENAME));
        }
    }

    @Nested
    @DisplayName("Reserved names")
    class ReservedNames {

        @Test
        @DisplayName("rejects reserved name CON")
        void reservedCon() {
            assertThatThrownBy(() -> maliciousFileChecks.checkFilename("con.pdf"))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getCode())
                            .isEqualTo(UploadSecurityErrors.CODE_SUSPICIOUS_FILENAME));
        }

        @Test
        @DisplayName("rejects reserved name NUL")
        void reservedNul() {
            assertThatThrownBy(() -> maliciousFileChecks.checkFilename("nul.txt"))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getCode())
                            .isEqualTo(UploadSecurityErrors.CODE_SUSPICIOUS_FILENAME));
        }

        @Test
        @DisplayName("rejects reserved name COM1")
        void reservedCom1() {
            assertThatThrownBy(() -> maliciousFileChecks.checkFilename("com1.pdf"))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getCode())
                            .isEqualTo(UploadSecurityErrors.CODE_SUSPICIOUS_FILENAME));
        }
    }

    @Nested
    @DisplayName("Dangerous and masked extensions")
    class DangerousExtensions {

        @Test
        @DisplayName("rejects .exe extension")
        void exeExtension() {
            assertThatThrownBy(() -> maliciousFileChecks.checkFilename("virus.exe"))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getCode())
                            .isEqualTo(UploadSecurityErrors.CODE_SUSPICIOUS_FILENAME));
        }

        @Test
        @DisplayName("rejects double extension masking (pdf.exe)")
        void doubleExtensionPdfExe() {
            assertThatThrownBy(() -> maliciousFileChecks.checkFilename("doc.pdf.exe"))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getCode())
                            .isEqualTo(UploadSecurityErrors.CODE_SUSPICIOUS_FILENAME));
        }

        @Test
        @DisplayName("rejects .bat extension")
        void batExtension() {
            assertThatThrownBy(() -> maliciousFileChecks.checkFilename("script.bat"))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getCode())
                            .isEqualTo(UploadSecurityErrors.CODE_SUSPICIOUS_FILENAME));
        }

        @Test
        @DisplayName("rejects .js extension (script)")
        void jsExtension() {
            assertThatThrownBy(() -> maliciousFileChecks.checkFilename("malicious.js"))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getCode())
                            .isEqualTo(UploadSecurityErrors.CODE_SUSPICIOUS_FILENAME));
        }

        @Test
        @DisplayName("rejects .jar extension")
        void jarExtension() {
            assertThatThrownBy(() -> maliciousFileChecks.checkFilename("app.jar"))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getCode())
                            .isEqualTo(UploadSecurityErrors.CODE_SUSPICIOUS_FILENAME));
        }

        @Test
        @DisplayName("rejects too many dots (masking)")
        void tooManyDots() {
            assertThatThrownBy(() -> maliciousFileChecks.checkFilename("a.b.c.pdf"))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getCode())
                            .isEqualTo(UploadSecurityErrors.CODE_SUSPICIOUS_FILENAME));
        }
    }

    @Nested
    @DisplayName("Filename length")
    class FilenameLength {

        @Test
        @DisplayName("rejects filename exceeding max length")
        void tooLong() {
            String longName = "a".repeat(201);
            assertThatThrownBy(() -> maliciousFileChecks.checkFilename(longName))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getCode())
                            .isEqualTo(UploadSecurityErrors.CODE_SUSPICIOUS_FILENAME));
        }
    }

    @Nested
    @DisplayName("Valid filenames")
    class ValidFilenames {

        @Test
        @DisplayName("accepts simple PDF filename")
        void simplePdf() {
            assertThatCode(() -> maliciousFileChecks.checkFilename("document.pdf"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("accepts filename with one dot in name")
        void oneDotInName() {
            assertThatCode(() -> maliciousFileChecks.checkFilename("my.file.pdf"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("accepts two dots (name and extension)")
        void twoDotsAllowed() {
            assertThatCode(() -> maliciousFileChecks.checkFilename("my.file.pdf"))
                    .doesNotThrowAnyException();
        }
    }
}
