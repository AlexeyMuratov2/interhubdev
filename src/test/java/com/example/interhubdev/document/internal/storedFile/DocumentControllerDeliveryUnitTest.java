package com.example.interhubdev.document.internal.storedFile;

import com.example.interhubdev.auth.AuthApi;
import com.example.interhubdev.document.DocumentApi;
import com.example.interhubdev.document.StoredFileDto;
import com.example.interhubdev.storedfile.FileStatus;
import com.example.interhubdev.storedfile.internal.StoredFileErrors;
import com.example.interhubdev.user.Role;
import com.example.interhubdev.user.UserDto;
import com.example.interhubdev.user.UserStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentController controlled delivery")
class DocumentControllerDeliveryUnitTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private DocumentApi documentApi;

    @Mock
    private AuthApi authApi;

    @Mock
    private HttpServletRequest request;

    @Test
    @DisplayName("download forces attachment headers and octet-stream content type")
    void downloadForcesHardenedHeaders() {
        UUID fileId = UUID.randomUUID();
        DocumentController controller = new DocumentController(documentApi, authApi);
        when(authApi.getCurrentUser(any())).thenReturn(Optional.of(authenticatedUser()));
        when(documentApi.getStoredFile(fileId)).thenReturn(Optional.of(new StoredFileDto(
            fileId,
            7,
            "application/pdf",
            "report.pdf",
            LocalDateTime.now(),
            USER_ID,
            FileStatus.ACTIVE
        )));
        when(documentApi.downloadByStoredFileId(fileId, USER_ID))
            .thenReturn(new ByteArrayInputStream("content".getBytes()));

        ResponseEntity<InputStreamResource> response = controller.download(fileId, request);

        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE)).isEqualTo("application/octet-stream");
        assertThat(response.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).contains("attachment;");
    }

    @Test
    @DisplayName("preview url is denied by delivery policy")
    void previewDeniedByPolicy() {
        UUID fileId = UUID.randomUUID();
        DocumentController controller = new DocumentController(documentApi, authApi);
        when(authApi.getCurrentUser(any())).thenReturn(Optional.of(authenticatedUser()));
        when(documentApi.getPreviewUrl(fileId, 3600, USER_ID))
            .thenThrow(StoredFileErrors.deliveryNotAllowed());

        assertThatThrownBy(() -> controller.getPreviewUrl(fileId, 3600, request))
            .hasMessageContaining("Delivery not allowed");
    }

    @Test
    @DisplayName("download url is denied by delivery policy")
    void downloadUrlDeniedByPolicy() {
        UUID fileId = UUID.randomUUID();
        DocumentController controller = new DocumentController(documentApi, authApi);
        when(authApi.getCurrentUser(any())).thenReturn(Optional.of(authenticatedUser()));
        when(documentApi.getDownloadUrl(fileId, 3600, USER_ID))
            .thenThrow(StoredFileErrors.deliveryNotAllowed());

        assertThatThrownBy(() -> controller.getDownloadUrl(fileId, 3600, request))
            .hasMessageContaining("Delivery not allowed");
    }

    private static UserDto authenticatedUser() {
        return new UserDto(
            USER_ID,
            "user@test.com",
            List.of(Role.STUDENT),
            UserStatus.ACTIVE,
            "Test",
            "User",
            null,
            null,
            LocalDateTime.now(),
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }
}
