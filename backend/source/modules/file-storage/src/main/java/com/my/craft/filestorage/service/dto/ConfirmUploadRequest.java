package com.my.craft.filestorage.service.dto;

import jakarta.validation.constraints.Min;

/**
 * @param size Byte size of the uploaded object, as observed by the client that performed the
 *     presigned {@code PUT} -- the backend never sees the object bytes for a presigned upload, so
 *     this is the only place file size is recorded.
 */
public record ConfirmUploadRequest(@Min(0) long size) {
}
