package com.my.craft.filestorage.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.my.craft.security.security.UserContext;

import com.my.craft.filestorage.config.FileStorageProperties;
import com.my.craft.filestorage.file.AuditActor;
import com.my.craft.filestorage.file.FileMetadataStore;
import com.my.craft.filestorage.file.FileNotFoundException;
import com.my.craft.filestorage.file.FileStatus;
import com.my.craft.filestorage.file.StoredFile;
import com.my.craft.filestorage.service.dto.ConfirmUploadRequest;
import com.my.craft.filestorage.service.dto.CreateUploadRequest;
import com.my.craft.filestorage.service.dto.DownloadUrlResponse;
import com.my.craft.filestorage.service.dto.FileSummaryResponse;
import com.my.craft.filestorage.service.dto.UploadUrlResponse;
import com.my.craft.filestorage.storage.PresignedUrl;
import com.my.craft.filestorage.storage.StorageProvider;

@Service
public class DefaultFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(DefaultFileStorageService.class);

    private final StorageProvider storageProvider;
    private final FileMetadataStore metadataStore;
    private final FileStorageProperties properties;

    public DefaultFileStorageService(
            StorageProvider storageProvider, FileMetadataStore metadataStore, FileStorageProperties properties) {
        this.storageProvider = storageProvider;
        this.metadataStore = metadataStore;
        this.properties = properties;
    }

    @Override
    public UploadUrlResponse createUploadUrl(CreateUploadRequest request, UserContext actor) {
        UUID fileId = UUID.randomUUID();
        String key = buildKey(fileId, request.fileName());

        PresignedUrl presigned =
                storageProvider.presignUpload(key, request.contentType(), properties.getUpload().getExpiration());

        metadataStore.save(new StoredFile(
                fileId,
                key,
                request.fileName(),
                request.contentType(),
                request.temporary(),
                Instant.now(),
                AuditActor.from(actor),
                FileStatus.PENDING));

        log.info("file {} (key={}) upload url issued by user {}", fileId, key, actor.userId());
        return new UploadUrlResponse(fileId, presigned.url(), key, presigned.expiresAt());
    }

    @Override
    public void confirm(UUID fileId, ConfirmUploadRequest request, UserContext actor) {
        StoredFile file = findOrThrow(fileId);
        file.setStatus(FileStatus.CONFIRMED);
        file.setConfirmedBy(AuditActor.from(actor));
        file.setSize(request.size());
        metadataStore.save(file);
        log.info("file {} (key={}) confirmed by user {}", fileId, file.getKey(), actor.userId());
    }

    @Override
    public DownloadUrlResponse createDownloadUrl(UUID fileId, UserContext actor) {
        StoredFile file = findOrThrow(fileId);
        PresignedUrl presigned = storageProvider.presignDownload(file.getKey(), properties.getDownload().getExpiration());
        log.info("file {} (key={}) download url issued to user {}", fileId, file.getKey(), actor.userId());
        return new DownloadUrlResponse(presigned.url(), presigned.expiresAt());
    }

    @Override
    public void delete(UUID fileId, UserContext actor) {
        StoredFile file = findOrThrow(fileId);
        storageProvider.delete(file.getKey());
        metadataStore.deleteById(fileId);
        log.info("file {} (key={}) deleted by user {}", fileId, file.getKey(), actor.userId());
    }

    @Override
    public List<FileSummaryResponse> listFiles(UserContext actor) {
        log.info("Listing files for user {}", actor.userId());
        return metadataStore.findAllConfirmed().stream()
                .map(file -> new FileSummaryResponse(
                        file.getId(),
                        file.getOriginalFileName(),
                        file.getContentType(),
                        file.getSize(),
                        file.getCreatedAt(),
                        file.getConfirmedBy() != null ? file.getConfirmedBy().username() : null))
                .toList();
    }

    private StoredFile findOrThrow(UUID fileId) {
        return metadataStore.findById(fileId).orElseThrow(() -> new FileNotFoundException(fileId));
    }

    private String buildKey(UUID fileId, String fileName) {
        String sanitized = fileName.replaceAll("[^A-Za-z0-9._-]", "_");
        return "%s/%s-%s".formatted(properties.getKeyPrefix(), fileId, sanitized);
    }
}
