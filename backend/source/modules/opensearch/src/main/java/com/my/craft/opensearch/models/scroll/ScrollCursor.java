package com.my.craft.opensearch.models.scroll;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * What the client sees as {@code scrollId}: OpenSearch's scroll id plus the index it was opened on
 * (so a cursor can't be replayed against a different search) and how many hits were already
 * returned (so the last page is detected against {@code hits.total} without an extra empty call).
 */
record ScrollCursor(String index, String openSearchScrollId, long seen) {

    private static final String SEPARATOR = "|";

    String encode() {
        String raw = seen + SEPARATOR + index + SEPARATOR + openSearchScrollId;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    static ScrollCursor decode(String token) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = raw.split("\\|", 3);
            return new ScrollCursor(parts[1], parts[2], Long.parseLong(parts[0]));
        } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("malformed scrollId", e);
        }
    }
}
