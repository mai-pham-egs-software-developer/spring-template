package com.my.craft.opensearch.models;

/**
 * Paging part of a scroll-paged search -- what to search for is passed separately. First page:
 * {@code scrollId} null; the search criteria and {@code size} are fixed into the scroll context
 * here. Next page: the previous {@link ScrollResponse#scrollId()}; OpenSearch replays the original
 * query, so criteria and {@code size} are ignored after the first page. {@code ttl} (seconds) is
 * how long the scroll context stays alive after this request.
 */
public record ScrollRequest(String scrollId, int size, long ttl) {

    public static final int MAX_SIZE = 100;
    public static final long MAX_TTL_SECONDS = 3_600;

    public ScrollRequest {
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_SIZE);
        }
        if (ttl < 1 || ttl > MAX_TTL_SECONDS) {
            throw new IllegalArgumentException("ttl must be between 1 and " + MAX_TTL_SECONDS + " seconds");
        }
        if (scrollId != null && scrollId.isBlank()) {
            scrollId = null;
        }
    }
}
