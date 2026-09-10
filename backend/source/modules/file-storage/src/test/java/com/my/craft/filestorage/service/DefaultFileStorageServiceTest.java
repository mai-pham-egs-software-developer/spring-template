package com.my.craft.filestorage.service;

import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.my.craft.security.user.UserContext;

import com.my.craft.filestorage.config.FileStorageProperties;
import com.my.craft.filestorage.file.FileNotFoundException;
import com.my.craft.filestorage.file.FileStatus;
import com.my.craft.filestorage.file.InMemoryFileMetadataStore;
import com.my.craft.filestorage.service.dto.ConfirmUploadRequest;
import com.my.craft.filestorage.service.dto.CreateUploadRequest;
import com.my.craft.filestorage.service.dto.FileSummaryResponse;
import com.my.craft.filestorage.service.dto.UploadUrlResponse;
import com.my.craft.filestorage.storage.PresignedUrl;
import com.my.craft.filestorage.storage.StorageProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultFileStorageServiceTest {

    private final RecordingStorageProvider storageProvider = new RecordingStorageProvider();
    private final InMemoryFileMetadataStore metadataStore = new InMemoryFileMetadataStore();
    private final FileStorageProperties properties = new FileStorageProperties();
    private final DefaultFileStorageService service =
            new DefaultFileStorageService(storageProvider, metadataStore, properties);
    private final UserContext actor = new UserContext("user-1", "alice", "alice@example.com", Set.of(), Map.of());

    @Test
    void createUploadUrl_registersPendingFileUnderPrefixedKeyAndActor() {
        UploadUrlResponse response =
                service.createUploadUrl(new CreateUploadRequest("photo.png", "image/png", true), actor);

        assertThat(response.key()).startsWith(properties.getKeyPrefix() + "/" + response.fileId());
        assertThat(response.key()).endsWith("-photo.png");
        assertThat(metadataStore.findById(response.fileId()))
                .get()
                .satisfies(file -> assertThat(file.getCreatedBy().userId()).isEqualTo(actor.userId()));
    }

    @Test
    void confirm_thenDelete_removesObjectAndMetadata() {
        UploadUrlResponse upload =
                service.createUploadUrl(new CreateUploadRequest("doc.pdf", "application/pdf", false), actor);

        service.confirm(upload.fileId(), new ConfirmUploadRequest(2048), actor);
        assertThat(metadataStore.findById(upload.fileId()))
                .get()
                .satisfies(file -> {
                    assertThat(file.getStatus()).isEqualTo(FileStatus.CONFIRMED);
                    assertThat(file.getConfirmedBy().userId()).isEqualTo(actor.userId());
                    assertThat(file.getSize()).isEqualTo(2048);
                });

        service.delete(upload.fileId(), actor);
        assertThat(metadataStore.findById(upload.fileId())).isEmpty();
        assertThat(storageProvider.deletedKeys).contains(upload.key());
    }

    @Test
    void confirm_unknownFileId_throwsFileNotFoundException() {
        assertThatThrownBy(() -> service.confirm(UUID.randomUUID(), new ConfirmUploadRequest(0), actor))
                .isInstanceOf(FileNotFoundException.class);
    }

    @Test
    void listFiles_returnsOnlyConfirmedFilesMostRecentFirst() {
        UploadUrlResponse pending = service.createUploadUrl(new CreateUploadRequest("draft.txt", "text/plain", true), actor);
        UploadUrlResponse confirmed =
                service.createUploadUrl(new CreateUploadRequest("report.pdf", "application/pdf", false), actor);
        service.confirm(confirmed.fileId(), new ConfirmUploadRequest(4096), actor);

        List<FileSummaryResponse> files = service.listFiles(actor);

        assertThat(files).extracting(FileSummaryResponse::fileId).containsExactly(confirmed.fileId());
        assertThat(files).extracting(FileSummaryResponse::fileId).doesNotContain(pending.fileId());
        assertThat(files.get(0).size()).isEqualTo(4096);
        assertThat(files.get(0).uploadedBy()).isEqualTo(actor.username());
    }

    private static class RecordingStorageProvider implements StorageProvider {

        private final Set<String> deletedKeys = new HashSet<>();

        @Override
        public PresignedUrl presignUpload(String key, String contentType, Duration expiration) {
            return presignedUrl(key, expiration);
        }

        @Override
        public PresignedUrl presignDownload(String key, Duration expiration) {
            return presignedUrl(key, expiration);
        }

        @Override
        public boolean exists(String key) {
            return !deletedKeys.contains(key);
        }

        @Override
        public void delete(String key) {
            deletedKeys.add(key);
        }

        private PresignedUrl presignedUrl(String key, Duration expiration) {
            try {
                URL url = URI.create("https://example-bucket.s3.amazonaws.com/" + key).toURL();
                return new PresignedUrl(url, key, Instant.now().plus(expiration));
            } catch (java.net.MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
