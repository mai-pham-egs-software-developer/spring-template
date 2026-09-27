package com.my.craft.opensearch.models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.Query;

/**
 * {@code keyword} matched ({@code multi_match}) over {@code fields}, AND every {@code filters}
 * entry matched exactly ({@code terms}: the field equals any of the listed values). A blank
 * {@code keyword} matches everything the filters allow.
 */
public record KeywordSearchCriteria(String keyword, List<String> fields, Map<String, List<String>> filters)
        implements SearchCriteria {

    public KeywordSearchCriteria {
        fields = fields == null ? List.of() : List.copyOf(fields);
        filters = filters == null ? Map.of() : Map.copyOf(filters);
        if (keyword != null && !keyword.isBlank() && fields.isEmpty()) {
            throw new IllegalArgumentException("fields are required when a keyword is given");
        }
    }

    public static KeywordSearchCriteria of(String keyword, String... fields) {
        return new KeywordSearchCriteria(keyword, List.of(fields), Map.of());
    }

    public KeywordSearchCriteria withFilter(String field, String... values) {
        Map<String, List<String>> combined = new HashMap<>(filters);
        combined.put(field, List.of(values));
        return new KeywordSearchCriteria(keyword, fields, combined);
    }

    @Override
    public Query toQuery() {
        List<Query> filterQueries = filters.entrySet().stream()
                .map(e -> {
                    List<FieldValue> values = e.getValue().stream().map(FieldValue::of).toList();
                    return Query.of(q -> q.terms(t -> t.field(e.getKey()).terms(tt -> tt.value(values))));
                })
                .toList();
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        return Query.of(q -> q.bool(b -> {
            b.filter(filterQueries);
            if (hasKeyword) {
                b.must(m -> m.multiMatch(mm -> mm.fields(fields).query(keyword)));
            }
            return b;
        }));
    }
}
