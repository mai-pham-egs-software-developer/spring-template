package com.my.craft.filestorage.file;

import com.my.craft.security.security.UserContext;

/**
 * Who performed an auditable action on a {@link StoredFile} (upload, confirm, ...). Trimmed down
 * from {@link UserContext} to just the fields worth keeping alongside file metadata.
 */
public record AuditActor(String userId, String username) {

    public static AuditActor from(UserContext userContext) {
        return new AuditActor(userContext.userId(), userContext.username());
    }
}
