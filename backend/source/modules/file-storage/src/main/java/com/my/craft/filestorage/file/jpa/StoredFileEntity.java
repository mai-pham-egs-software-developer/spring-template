package com.my.craft.filestorage.file.jpa;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.my.craft.filestorage.file.FileStatus;

/**
 * JPA row for a {@link com.my.craft.filestorage.file.StoredFile} -- table {@code stored_file}.
 * Deliberately separate from the domain type: {@code StoredFile} stays framework-agnostic and
 * {@link JpaFileMetadataStore} maps between the two, so nothing outside this {@code jpa}
 * subpackage needs to know persistence is JPA at all.
 */
@Entity
@Table(name = "stored_file")
public class StoredFileEntity {

    @Id
    private UUID id;

    @Column(name = "object_key", nullable = false, unique = true)
    private String key;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(nullable = false)
    private boolean temporary;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by_user_id", nullable = false)
    private String createdByUserId;

    @Column(name = "created_by_username", nullable = false)
    private String createdByUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FileStatus status;

    @Column(name = "confirmed_by_user_id")
    private String confirmedByUserId;

    @Column(name = "confirmed_by_username")
    private String confirmedByUsername;

    @Column(nullable = false)
    private long size;

    /** JPA. */
    protected StoredFileEntity() {}

    public StoredFileEntity(
            UUID id,
            String key,
            String originalFileName,
            String contentType,
            boolean temporary,
            Instant createdAt,
            String createdByUserId,
            String createdByUsername,
            FileStatus status,
            long size) {
        this.id = id;
        this.key = key;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.temporary = temporary;
        this.createdAt = createdAt;
        this.createdByUserId = createdByUserId;
        this.createdByUsername = createdByUsername;
        this.status = status;
        this.size = size;
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

    public String getCreatedByUserId() {
        return createdByUserId;
    }

    public String getCreatedByUsername() {
        return createdByUsername;
    }

    public FileStatus getStatus() {
        return status;
    }

    public void setStatus(FileStatus status) {
        this.status = status;
    }

    public String getConfirmedByUserId() {
        return confirmedByUserId;
    }

    public void setConfirmedByUserId(String confirmedByUserId) {
        this.confirmedByUserId = confirmedByUserId;
    }

    public String getConfirmedByUsername() {
        return confirmedByUsername;
    }

    public void setConfirmedByUsername(String confirmedByUsername) {
        this.confirmedByUsername = confirmedByUsername;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
