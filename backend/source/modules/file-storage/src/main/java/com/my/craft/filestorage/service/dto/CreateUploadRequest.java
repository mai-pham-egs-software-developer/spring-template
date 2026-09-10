package com.my.craft.filestorage.service.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * @param temporary Marks the upload as a temp file eligible for the cleanup schedule until
 *     {@link com.my.craft.filestorage.service.FileStorageService#confirm} is called.
 */
public record CreateUploadRequest(@NotBlank String fileName, @NotBlank String contentType, boolean temporary) {
}
