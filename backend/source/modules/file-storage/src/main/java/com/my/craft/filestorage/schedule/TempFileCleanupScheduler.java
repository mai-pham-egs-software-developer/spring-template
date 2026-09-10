package com.my.craft.filestorage.schedule;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.my.craft.filestorage.config.FileStorageProperties;
import com.my.craft.filestorage.file.FileMetadataStore;
import com.my.craft.filestorage.file.StoredFile;
import com.my.craft.filestorage.storage.StorageProvider;

/**
 * Purges temp uploads ({@code temporary=true} on the original upload request) that were never
 * {@link com.my.craft.filestorage.service.FileStorageService#confirm confirmed} before
 * {@code file-storage.temp.ttl} elapsed -- both the object in storage and its {@link StoredFile}
 * metadata. Runs on {@code file-storage.temp.cleanup-cron}.
 */
@Component
public class TempFileCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(TempFileCleanupScheduler.class);

    private final FileMetadataStore metadataStore;
    private final StorageProvider storageProvider;
    private final FileStorageProperties properties;

    public TempFileCleanupScheduler(
            FileMetadataStore metadataStore, StorageProvider storageProvider, FileStorageProperties properties) {
        this.metadataStore = metadataStore;
        this.storageProvider = storageProvider;
        this.properties = properties;
    }

    @Scheduled(cron = "${file-storage.temp.cleanup-cron:0 0 * * * *}")
    public void cleanUpExpiredTempFiles() {
        Instant threshold = Instant.now().minus(properties.getTemp().getTtl());
        List<StoredFile> expired = metadataStore.findExpiredTemp(threshold);
        for (StoredFile file : expired) {
            try {
                storageProvider.delete(file.getKey());
                metadataStore.deleteById(file.getId());
            } catch (RuntimeException e) {
                log.warn("failed to clean up expired temp file {} (key={})", file.getId(), file.getKey(), e);
            }
        }
    }
}
