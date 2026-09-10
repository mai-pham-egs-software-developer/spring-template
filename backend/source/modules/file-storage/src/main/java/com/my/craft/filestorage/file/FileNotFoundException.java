package com.my.craft.filestorage.file;

import java.util.UUID;

/** No {@link StoredFile} is registered for the given file id. */
public class FileNotFoundException extends RuntimeException {

    public FileNotFoundException(UUID fileId) {
        super("file not found: " + fileId);
    }
}
