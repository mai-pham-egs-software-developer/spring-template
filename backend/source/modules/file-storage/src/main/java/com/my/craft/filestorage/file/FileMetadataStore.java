package com.my.craft.filestorage.file;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Where {@link StoredFile} records live. {@link InMemoryFileMetadataStore} is the only
 * implementation today (single-instance, non-durable -- see docs/file-storage.md); swap in a
 * {@code modules/persistent}-backed implementation once that module exists, without touching
 * {@code FileStorageService} or the cleanup schedule, which only depend on this interface.
 */
public interface FileMetadataStore {

    void save(StoredFile file);

    Optional<StoredFile> findById(UUID id);

    /** Temp uploads still {@link FileStatus#PENDING} and created before {@code threshold}. */
    List<StoredFile> findExpiredTemp(Instant threshold);

    /** Every {@link FileStatus#CONFIRMED} file, most recently created first. */
    List<StoredFile> findAllConfirmed();

    void deleteById(UUID id);
}
