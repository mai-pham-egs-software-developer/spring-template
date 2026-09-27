package com.my.craft.opensearch.models;

/**
 * Classic offset paging ({@code from}/{@code size}), 0-based {@code page}. Unlike {@link
 * PitRequest} each page reads the live index, so documents indexed or deleted between requests
 * can shift results across pages; use it when random access to page N matters more than that.
 */
public record PageRequest(int page, int size) {

    public static final int MAX_SIZE = 100;

    /** OpenSearch's default {@code index.max_result_window}: {@code from + size} can't exceed it. */
    public static final int MAX_RESULT_WINDOW = 10_000;

    public PageRequest {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_SIZE);
        }
        if ((long) (page + 1) * size > MAX_RESULT_WINDOW) {
            throw new IllegalArgumentException("page * size exceeds the max result window of " + MAX_RESULT_WINDOW);
        }
    }

    public int from() {
        return page * size;
    }
}
