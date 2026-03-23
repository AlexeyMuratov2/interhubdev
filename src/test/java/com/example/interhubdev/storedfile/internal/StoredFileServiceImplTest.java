package com.example.interhubdev.storedfile.internal;

import com.example.interhubdev.error.AppException;
import com.example.interhubdev.storedfile.DeliveryContext;
import com.example.interhubdev.storedfile.FileSafetyClass;
import com.example.interhubdev.storedfile.FileStatus;
import com.example.interhubdev.storedfile.StoredFile;
import com.example.interhubdev.storedfile.StoredFileMeta;
import com.example.interhubdev.storedfile.UploadContextKey;
import com.example.interhubdev.storedfile.internal.policy.ClassificationPolicy;
import com.example.interhubdev.storedfile.internal.policy.DeliveryPolicyEvaluator;
import com.example.interhubdev.storedfile.internal.uploadSecurity.UploadContext;
import com.example.interhubdev.storedfile.internal.uploadSecurity.UploadSecurityPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StoredFileServiceImpl")
class StoredFileServiceImplTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private UploadSecurityPort uploadSecurityPort;
    @Mock
    private ClassificationPolicy classificationPolicy;
    @Mock
    private DeliveryPolicyEvaluator deliveryPolicyEvaluator;
    @Mock
    private StoragePort storagePort;
    @Mock
    private StoredFileRepository storedFileRepository;

    @Test
    @DisplayName("stores general user file under opaque key and hardened object metadata")
    void uploadUsesOpaquePathAndBinaryStorageMetadata() throws Exception {
        StoredFileServiceImpl service = new StoredFileServiceImpl(
            uploadSecurityPort,
            classificationPolicy,
            deliveryPolicyEvaluator,
            storagePort,
            storedFileRepository,
            new FileValidation(),
            List.of()
        );
        Path tempFile = Files.createTempFile("storedfile-test-", ".bin");
        Files.write(tempFile, "payload".getBytes());
        try {
            when(classificationPolicy.classify(UploadContextKey.GENERAL_USER_FILE))
                .thenReturn(FileSafetyClass.GENERAL_USER_ATTACHMENT_ONLY);
            when(storagePort.upload(anyString(), any(), anyString(), anyLong()))
                .thenAnswer(invocation -> new StoragePort.UploadResult(
                    invocation.getArgument(0),
                    invocation.getArgument(3),
                    invocation.getArgument(2)
                ));
            when(storedFileRepository.save(any(StoredFile.class))).thenAnswer(invocation -> invocation.getArgument(0));

            StoredFileMeta meta = service.upload(
                tempFile,
                "dangerous.exe",
                "application/x-msdownload",
                Files.size(tempFile),
                USER_ID,
                UploadContextKey.GENERAL_USER_FILE
            );

            ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> contentTypeCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<StoredFile> entityCaptor = ArgumentCaptor.forClass(StoredFile.class);
            verify(storagePort).upload(pathCaptor.capture(), any(), contentTypeCaptor.capture(), eq(Files.size(tempFile)));
            verify(storedFileRepository).save(entityCaptor.capture());

            assertThat(contentTypeCaptor.getValue()).isEqualTo("application/octet-stream");
            assertThat(pathCaptor.getValue()).doesNotContain("dangerous.exe");
            assertThat(entityCaptor.getValue().getContentType()).isEqualTo("application/x-msdownload");
            assertThat(meta.contentType()).isEqualTo("application/x-msdownload");
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    @DisplayName("denies presigned URL for general user attachment even when stream delivery is allowed")
    void deniesPresignedUrlForGeneralUserAttachment() {
        StoredFileServiceImpl service = new StoredFileServiceImpl(
            uploadSecurityPort,
            classificationPolicy,
            deliveryPolicyEvaluator,
            storagePort,
            storedFileRepository,
            new FileValidation(),
            List.of()
        );
        UUID id = UUID.randomUUID();
        StoredFile entity = StoredFile.builder()
            .id(id)
            .storagePath("files/2026/3/" + id)
            .size(10)
            .contentType("application/pdf")
            .originalName("report.pdf")
            .uploadedAt(LocalDateTime.now())
            .uploadedBy(USER_ID)
            .status(FileStatus.ACTIVE)
            .safetyClass(FileSafetyClass.GENERAL_USER_ATTACHMENT_ONLY)
            .uploadContextKey(UploadContextKey.GENERAL_USER_FILE)
            .build();

        when(storedFileRepository.findById(id)).thenReturn(Optional.of(entity));
        when(deliveryPolicyEvaluator.isDeliveryAllowed(FileSafetyClass.GENERAL_USER_ATTACHMENT_ONLY, DeliveryContext.ATTACHMENT_ONLY))
            .thenReturn(true);

        assertThatThrownBy(() -> service.getPresignedUrl(id, 60, DeliveryContext.ATTACHMENT_ONLY))
            .isInstanceOf(AppException.class)
            .hasMessageContaining("Delivery not allowed");

        verify(storagePort, never()).generatePreviewUrl(anyString(), anyInt());
    }
}
