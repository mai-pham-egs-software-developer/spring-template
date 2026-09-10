package com.my.craft.filestorage.storage;

import java.time.Duration;

/**
 * Abstraction over the object-storage backend. {@link S3StorageProvider} is the only
 * implementation today (it talks to real AWS S3 or any S3-compatible service such as MinIO, purely
 * through {@code file-storage.s3.*} config -- see docs/file-storage.md), but callers
 * ({@code FileStorageService}, the cleanup schedule) only ever depend on this interface, so a
 * non-S3 backend (e.g. Azure Blob, GCS) can be swapped in later as another bean of this type
 * without touching them.
 */
public interface StorageProvider {

    /** Presigned URL a client can PUT the object bytes to directly. */
    PresignedUrl presignUpload(String key, String contentType, Duration expiration);

    /** Presigned URL a client can GET the object bytes from directly. */
    PresignedUrl presignDownload(String key, Duration expiration);

    boolean exists(String key);

    void delete(String key);
}
