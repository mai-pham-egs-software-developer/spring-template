package com.my.craft.filestorage.file.jpa;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.my.craft.filestorage.file.AuditActor;
import com.my.craft.filestorage.file.FileMetadataStore;
import com.my.craft.filestorage.file.FileStatus;
import com.my.craft.filestorage.file.StoredFile;

/**
 * {@link FileMetadataStore} backed by {@link StoredFileJpaRepository} -- the implementation
 * actually wired at runtime (see {@code FileStorageConfig}'s {@code @EnableJpaRepositories}/
 * {@code @EntityScan}). Durable and shared across instances, unlike
 * {@code InMemoryFileMetadataStore}, which is kept around only for tests that don't need a
 * database.
 */
@Component
public class JpaFileMetadataStore implements FileMetadataStore {

    private final StoredFileJpaRepository repository;

    public JpaFileMetadataStore(StoredFileJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(StoredFile file) {
        repository.save(toEntity(file));
    }

    @Override
    public Optional<StoredFile> findById(UUID id) {
        return repository.findById(id).map(JpaFileMetadataStore::toDomain);
    }

    @Override
    public List<StoredFile> findExpiredTemp(Instant threshold) {
        return repository.findByTemporaryTrueAndStatusAndCreatedAtBefore(FileStatus.PENDING, threshold).stream()
                .map(JpaFileMetadataStore::toDomain)
                .toList();
    }

    @Override
    public List<StoredFile> findAllConfirmed() {
        return repository.findByStatusOrderByCreatedAtDesc(FileStatus.CONFIRMED).stream()
                .map(JpaFileMetadataStore::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    private static StoredFileEntity toEntity(StoredFile file) {
        StoredFileEntity entity = new StoredFileEntity(
                file.getId(),
                file.getKey(),
                file.getOriginalFileName(),
                file.getContentType(),
                file.isTemporary(),
                file.getCreatedAt(),
                file.getCreatedBy().userId(),
                file.getCreatedBy().username(),
                file.getStatus(),
                file.getSize());
        AuditActor confirmedBy = file.getConfirmedBy();
        if (confirmedBy != null) {
            entity.setConfirmedByUserId(confirmedBy.userId());
            entity.setConfirmedByUsername(confirmedBy.username());
        }
        return entity;
    }

    private static StoredFile toDomain(StoredFileEntity entity) {
        StoredFile file = new StoredFile(
                entity.getId(),
                entity.getKey(),
                entity.getOriginalFileName(),
                entity.getContentType(),
                entity.isTemporary(),
                entity.getCreatedAt(),
                new AuditActor(entity.getCreatedByUserId(), entity.getCreatedByUsername()),
                entity.getStatus());
        file.setSize(entity.getSize());
        if (entity.getConfirmedByUserId() != null) {
            file.setConfirmedBy(new AuditActor(entity.getConfirmedByUserId(), entity.getConfirmedByUsername()));
        }
        return file;
    }
}
