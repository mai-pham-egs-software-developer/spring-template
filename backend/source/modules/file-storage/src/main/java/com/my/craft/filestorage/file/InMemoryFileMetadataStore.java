package com.my.craft.filestorage.file;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * In-memory {@link FileMetadataStore}. Fine for a single-instance template/dev setup; metadata is
 * lost on restart and not shared across replicas -- see docs/file-storage.md.
 */
@Component
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
    public void deleteById(UUID id) {
        files.remove(id);
    }
}
