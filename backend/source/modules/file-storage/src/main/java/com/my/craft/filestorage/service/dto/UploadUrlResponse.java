package com.my.craft.filestorage.service.dto;

import java.net.URL;
import java.time.Instant;
import java.util.UUID;

public record UploadUrlResponse(UUID fileId, URL uploadUrl, String key, Instant expiresAt) {
}
