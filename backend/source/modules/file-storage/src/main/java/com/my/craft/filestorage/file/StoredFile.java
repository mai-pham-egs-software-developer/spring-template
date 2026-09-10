package com.my.craft.filestorage.file;

import java.time.Instant;
import java.util.UUID;

/**
 * Metadata tracked per file id, from presigned-upload issuance through confirmation/deletion. See
 * {@link FileMetadataStore}.
 */
public class StoredFile {

    private final UUID id;
    private final String key;
    private final String originalFileName;
    private final String contentType;
    private final boolean temporary;
    private final Instant createdAt;
    private final AuditActor createdBy;
    private FileStatus status;
    private AuditActor confirmedBy;

    public StoredFile(
            UUID id,
            String key,
            String originalFileName,
            String contentType,
            boolean temporary,
            Instant createdAt,
            AuditActor createdBy,
            FileStatus status) {
        this.id = id;
        this.key = key;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.temporary = temporary;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public String getContentType() {
        return contentType;
    }

    public boolean isTemporary() {
        return temporary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public AuditActor getCreatedBy() {
        return createdBy;
    }

    public FileStatus getStatus() {
        return status;
    }

    public void setStatus(FileStatus status) {
        this.status = status;
    }

    public AuditActor getConfirmedBy() {
        return confirmedBy;
    }

    public void setConfirmedBy(AuditActor confirmedBy) {
        this.confirmedBy = confirmedBy;
    }
}
