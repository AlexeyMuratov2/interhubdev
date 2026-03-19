package com.example.interhubdev.storedfile.internal.uploadSecurity;

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
 * Unit tests for malicious filename checks.
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
                .satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo(UploadSecurityErrors.CODE_SUSPICIOUS_FILENAME));
        }

        @Test
        @DisplayName("rejects blank filename")
        void blankFilename() {
            assertThatThrownBy(() -> maliciousFileChecks.checkFilename("   "))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getCode()).isEqualTo(UploadSecurityErrors.CODE_SUSPICIOUS_FILENAME));
        }
    }

    @Nested
    @DisplayName("Valid filenames")
    class ValidFilenames {

        @Test
        @DisplayName("accepts simple PDF filename")
        void simplePdf() {
            assertThatCode(() -> maliciousFileChecks.checkFilename("document.pdf")).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("accepts filename with one dot in name")
        void oneDotInName() {
            assertThatCode(() -> maliciousFileChecks.checkFilename("my.file.pdf")).doesNotThrowAnyException();
        }
    }
}
