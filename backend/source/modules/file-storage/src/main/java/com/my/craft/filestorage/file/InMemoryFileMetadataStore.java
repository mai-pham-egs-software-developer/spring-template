package com.my.craft.filestorage.file;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory {@link FileMetadataStore}. Not a Spring bean -- {@code com.my.craft.filestorage.file
 * .jpa.JpaFileMetadataStore} is what's actually wired at runtime, backed by the {@code
 * stored_file} table (see docs/file-storage.md). Kept around for tests that want a
 * {@link FileMetadataStore} without a database; construct it directly with {@code new}.
 */
public class InMemoryFileMetadataStore implements FileMetadataStore {

    private final Map<UUID, StoredFile> files = new ConcurrentHashMap<>();

    @Override
    public void save(StoredFile file) {
        files.put(file.getId(), file);
    }

    @Override
    public Optional<StoredFile> findById(UUID id) {
        return Optional.ofNullable(files.get(id));
    }

    @Override
    public List<StoredFile> findExpiredTemp(Instant threshold) {
        return files.values().stream()
                .filter(StoredFile::isTemporary)
                .filter(file -> file.getStatus() == FileStatus.PENDING)
                .filter(file -> file.getCreatedAt().isBefore(threshold))
                .toList();
    }

    @Override
    public List<StoredFile> findAllConfirmed() {
        return files.values().stream()
                .filter(file -> file.getStatus() == FileStatus.CONFIRMED)
                .sorted(Comparator.comparing(StoredFile::getCreatedAt).reversed())
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        files.remove(id);
    }
}
