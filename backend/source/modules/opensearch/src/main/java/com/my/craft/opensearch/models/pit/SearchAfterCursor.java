package com.my.craft.opensearch.models.pit;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.opensearch.client.opensearch._types.FieldValue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * What the client sees as {@code pitId} for {@link SearchAfterPaging}: the index, OpenSearch's PIT
 * id and the last returned hit's sort values ({@code search_after}), as base64url JSON. {@code
 * searchAfter} is empty on the first page.
 */
record SearchAfterCursor(String index, String openSearchPitId, List<FieldValue> searchAfter) {

    private static final ObjectMapper JSON = new ObjectMapper();

    private record Wire(String index, String pit, List<Object> after) {}

    String encode() {
        List<Object> after = searchAfter.stream().map(SearchAfterCursor::toPlain).toList();
        try {
            byte[] raw = JSON.writeValueAsBytes(new Wire(index, openSearchPitId, after));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("cannot encode cursor", e);
        }
    }

    static SearchAfterCursor decode(String token) {
        try {
            Wire wire = JSON.readValue(Base64.getUrlDecoder().decode(token), Wire.class);
            List<FieldValue> after = new ArrayList<>();
            for (Object value : wire.after() == null ? List.of() : wire.after()) {
                after.add(fromPlain(value));
            }
            return new SearchAfterCursor(wire.index(), wire.pit(), List.copyOf(after));
        } catch (IllegalArgumentException | java.io.IOException e) {
            throw new IllegalArgumentException("malformed pitId", e);
        }
    }

    private static Object toPlain(FieldValue value) {
        return switch (value._kind()) {
            case String -> value.stringValue();
            case Long -> value.longValue();
            case Double -> value.doubleValue();
            case Boolean -> value.booleanValue();
            case Null -> null;
            default -> throw new IllegalStateException("unsupported sort value kind " + value._kind());
        };
    }

    private static FieldValue fromPlain(Object value) {
        if (value == null) {
            return FieldValue.NULL;
        }
        if (value instanceof String s) {
            return FieldValue.of(s);
        }
        if (value instanceof Boolean b) {
            return FieldValue.of(b);
        }
        if (value instanceof Integer || value instanceof Long) {
            return FieldValue.of(((Number) value).longValue());
        }
        if (value instanceof Number n) {
            return FieldValue.of(n.doubleValue());
        }
        throw new IllegalArgumentException("unsupported sort value " + value);
    }
}
