package com.my.craft.filestorage.service;

import java.util.List;
import java.util.UUID;

import com.my.craft.security.security.UserContext;

import com.my.craft.filestorage.service.dto.ConfirmUploadRequest;
import com.my.craft.filestorage.service.dto.CreateUploadRequest;
import com.my.craft.filestorage.service.dto.DownloadUrlResponse;
import com.my.craft.filestorage.service.dto.FileSummaryResponse;
import com.my.craft.filestorage.service.dto.UploadUrlResponse;

/**
 * Issues a file id up front and a presigned URL the caller uploads/downloads directly against the
 * storage backend with -- object bytes never pass through this service. Every method takes the
 * caller's {@link UserContext} so who did what is recorded on the {@code StoredFile} for audit
 * purposes. See docs/file-storage.md.
 */
public interface FileStorageService {

    UploadUrlResponse createUploadUrl(CreateUploadRequest request, UserContext actor);

    /** Marks the upload confirmed, exempting it from the temp-file cleanup schedule. */
    void confirm(UUID fileId, ConfirmUploadRequest request, UserContext actor);

    DownloadUrlResponse createDownloadUrl(UUID fileId, UserContext actor);

    void delete(UUID fileId, UserContext actor);

    /** Every confirmed file, most recently uploaded first. */
    List<FileSummaryResponse> listFiles(UserContext actor);
}
