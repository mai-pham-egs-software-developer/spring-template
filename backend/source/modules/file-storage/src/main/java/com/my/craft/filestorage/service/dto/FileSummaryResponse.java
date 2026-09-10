package com.my.craft.filestorage.service.dto;

import java.time.Instant;
import java.util.UUID;

/** One row of {@code GET /api/files} -- every {@code CONFIRMED} file, most recent first. */
public record FileSummaryResponse(
        UUID fileId, String fileName, String contentType, long size, Instant createdAt, String uploadedBy) {
}
