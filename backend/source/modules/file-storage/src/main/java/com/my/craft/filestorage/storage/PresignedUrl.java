package com.my.craft.filestorage.storage;

import java.net.URL;
import java.time.Instant;

/**
 * A time-boxed URL a client can use directly against the storage backend, without the request
 * ever passing through this service.
 */
public record PresignedUrl(URL url, String key, Instant expiresAt) {
}
