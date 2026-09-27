package com.my.craft.opensearch.models.pit;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.Pit;
import org.springframework.stereotype.Component;

import com.my.craft.opensearch.SearchClient;
import com.my.craft.opensearch.exception.OpenSearchOperationException;
import com.my.craft.opensearch.models.PitRequest;
import com.my.craft.opensearch.models.PitResponse;
import com.my.craft.opensearch.models.Sort;

/**
 * Point in time + {@code search_after} paging -- the "infinite scroll" strategy that, unlike the
 * scroll API, works with {@code collapse}:
 *
 * <ul>
 *   <li>First page opens a PIT (frozen snapshot); every later page reads the same snapshot.
 *   <li>The next page starts after the last returned hit's sort values, carried in the cursor
 *       ({@link SearchAfterCursor}) -- no offset, so no {@code index.max_result_window} limit.
 *   <li>{@code sort} must be total (last field unique per result). With {@code collapse},
 *       OpenSearch requires sorting on the collapse field alone -- e.g. collapse on {@code
 *       fileRefId} sorted by {@code fileRefId} pages whole files, each on exactly one page.
 *   <li>{@code size + 1} hits are fetched to detect the last page; the PIT is then deleted and the
 *       cursor is {@code null}.
 * </ul>
 */
@Component
public class SearchAfterPaging {

    private final SearchClient searchClient;

    public SearchAfterPaging(SearchClient searchClient) {
        this.searchClient = searchClient;
    }

    /** {@code query}: anything but pit, sort, search_after and size (query, collapse, highlight...). */
    public <D, T> PitResponse<T> search(
            PitRequest request,
            String index,
            Sort sort,
            UnaryOperator<SearchRequest.Builder> query,
            Class<D> documentType,
            Function<Hit<D>, T> mapper) {
        SearchAfterCursor cursor = cursorFor(request, index);
        int size = request.size();

        Pit pit = Pit.of(p -> p.id(cursor.openSearchPitId()).keepAlive(PitLifecycle.keepAlive(request.ttl())));
        SearchRequest searchRequest = SearchRequest.of(s -> {
            query.apply(s.pit(pit).sort(sort.toSortOptions()).size(size + 1));
            if (!cursor.searchAfter().isEmpty()) {
                s.searchAfter(cursor.searchAfter());
            }
            return s;
        });

        List<Hit<D>> hits;
        try {
            hits = searchClient.raw().search(searchRequest, documentType).hits().hits();
        } catch (IOException e) {
            throw new OpenSearchOperationException("search_after search failed on index " + index, e);
        }

        List<Hit<D>> page = hits.stream().limit(size).toList();
        List<T> data = page.stream().map(mapper).toList();
        if (hits.size() > size) {
            SearchAfterCursor next = new SearchAfterCursor(index, cursor.openSearchPitId(), page.get(size - 1).sort());
            return new PitResponse<>(data, next.encode(), request.ttl());
        }
        PitLifecycle.close(searchClient, cursor.openSearchPitId());
        return new PitResponse<>(data, null, 0);
    }

    private SearchAfterCursor cursorFor(PitRequest request, String index) {
        if (request.pitId() == null) {
            return new SearchAfterCursor(index, PitLifecycle.open(searchClient, index, request.ttl()), List.of());
        }
        SearchAfterCursor cursor = SearchAfterCursor.decode(request.pitId());
        if (!index.equals(cursor.index())) {
            throw new IllegalArgumentException("pitId belongs to a different search");
        }
        return cursor;
    }
}
