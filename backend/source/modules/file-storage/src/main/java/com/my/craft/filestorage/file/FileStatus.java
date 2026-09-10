package com.my.craft.filestorage.file;

/**
 * Lifecycle of a {@link StoredFile} between the presigned upload URL being issued and the caller
 * confirming the upload actually happened.
 */
public enum FileStatus {

    /** Upload URL issued, upload not yet confirmed. Eligible for temp cleanup once expired. */
    PENDING,

    /** Caller confirmed the object was uploaded; exempt from temp cleanup. */
    CONFIRMED
}
