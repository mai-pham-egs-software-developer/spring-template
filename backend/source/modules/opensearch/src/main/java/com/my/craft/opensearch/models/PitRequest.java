package com.my.craft.opensearch.models;

/**
 * Paging part of a point-in-time paged search -- what to search for is passed separately. First
 * page: {@code pitId} null. Next page: the previous {@link PitResponse#pitId()} with the SAME
 * search criteria -- the cursor already knows where the next page starts. {@code ttl} (seconds)
 * is how long the snapshot stays alive after this request.
 */
public record PitRequest(String pitId, int size, long ttl) {

    public static final int MAX_SIZE = 100;
    public static final long MAX_TTL_SECONDS = 3_600;

    public PitRequest {
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_SIZE);
        }
        if (ttl < 1 || ttl > MAX_TTL_SECONDS) {
            throw new IllegalArgumentException("ttl must be between 1 and " + MAX_TTL_SECONDS + " seconds");
        }
        if (pitId != null && pitId.isBlank()) {
            pitId = null;
        }
    }
}
