package com.my.craft.opensearch.models.pit;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * What the client sees as {@code pitId}: OpenSearch's own PIT id plus the index it was opened on
 * and the offset the next page starts at, base64url-encoded. OpenSearch's PIT is only a frozen
 * snapshot -- it doesn't remember a read position -- so the position travels in the cursor instead
 * of in server-side state.
 */
record PitCursor(String index, String openSearchPitId, int from) {

    private static final String SEPARATOR = "|";

    String encode() {
        String raw = from + SEPARATOR + index + SEPARATOR + openSearchPitId;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    PitCursor next(int size) {
        return new PitCursor(index, openSearchPitId, from + size);
    }

    static PitCursor decode(String token) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = raw.split("\\|", 3);
            return new PitCursor(parts[1], parts[2], Integer.parseInt(parts[0]));
        } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("malformed pitId", e);
        }
    }
}
