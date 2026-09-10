package com.my.craft.filestorage.file.jpa;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.my.craft.filestorage.file.FileStatus;

public interface StoredFileJpaRepository extends JpaRepository<StoredFileEntity, UUID> {

    List<StoredFileEntity> findByTemporaryTrueAndStatusAndCreatedAtBefore(FileStatus status, Instant threshold);

    List<StoredFileEntity> findByStatusOrderByCreatedAtDesc(FileStatus status);
}
