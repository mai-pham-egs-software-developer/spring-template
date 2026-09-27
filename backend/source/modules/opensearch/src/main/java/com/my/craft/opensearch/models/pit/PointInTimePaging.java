package com.my.craft.opensearch.models.pit;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import com.my.craft.opensearch.models.PitRequest;
import com.my.craft.opensearch.models.PitResponse;
import com.my.craft.opensearch.models.Sort;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.Pit;
import org.springframework.stereotype.Component;

import com.my.craft.opensearch.SearchClient;
import com.my.craft.opensearch.exception.OpenSearchOperationException;

/**
 * The point-in-time paging strategy shared by every {@link PitRequest}-based search:
 *
 * <ul>
 *   <li>First page opens a PIT (a frozen snapshot) with the request's {@code ttl}; every later page
 *       reads the same snapshot, so documents indexed or deleted mid-scroll don't shift pages.
 *   <li>Results are sorted by {@code _score} then a unique tiebreaker, so equal scores keep a fixed
 *       order between requests.
 *   <li>The next page's offset travels inside the returned cursor ({@link PitCursor}) -- {@code
 *       search_after} isn't an option because OpenSearch rejects it together with {@code collapse}
 *       unless sorting on the collapse field alone, which would lose relevance order; when that
 *       order is acceptable, use {@link SearchAfterPaging} instead (no {@code max_result_window}
 *       limit).
 *   <li>{@code size + 1} hits are fetched to know whether another page exists without a count
 *       query; on the last page the PIT is deleted and the cursor is {@code null}.
 * </ul>
 */
@Component
public class PointInTimePaging {

    /** OpenSearch's default {@code index.max_result_window}: {@code from + size} can't exceed it. */
    private static final int MAX_RESULT_WINDOW = 10_000;

    private final SearchClient searchClient;

    public PointInTimePaging(SearchClient searchClient) {
        this.searchClient = searchClient;
    }

    /**
     * Runs {@code query} (query/collapse/aggregations -- anything but pit, sort and paging) for one
     * page of {@code request}, mapping each hit with {@code mapper}. {@code sort}'s last field must
     * be unique per result (e.g. the collapse field or {@code refId}, see {@link
     * Sort#withTieBreaker}) so equal values keep a fixed order between pages.
     */
    public <D, T> PitResponse<T> search(
            PitRequest request,
            String index,
            Sort sort,
            UnaryOperator<SearchRequest.Builder> query,
            Class<D> documentType,
            Function<Hit<D>, T> mapper) {
        PitCursor cursor = cursorFor(request, index);
        int size = request.size();
        if (cursor.from() + size + 1 > MAX_RESULT_WINDOW) {
            throw new IllegalArgumentException("cannot page past " + MAX_RESULT_WINDOW + " results");
        }

        Pit pit = Pit.of(p -> p.id(cursor.openSearchPitId()).keepAlive(PitLifecycle.keepAlive(request.ttl())));
        SearchRequest searchRequest = SearchRequest.of(s -> query.apply(s.pit(pit)
                .sort(sort.toSortOptions())
                .from(cursor.from())
                .size(size + 1)));

        List<Hit<D>> hits;
        try {
            hits = searchClient.raw().search(searchRequest, documentType).hits().hits();
        } catch (IOException e) {
            throw new OpenSearchOperationException("point-in-time search failed on index " + index, e);
        }

        List<T> data = hits.stream().limit(size).map(mapper).toList();
        if (hits.size() > size) {
            return new PitResponse<>(data, cursor.next(size).encode(), request.ttl());
        }
        PitLifecycle.close(searchClient, cursor.openSearchPitId());
        return new PitResponse<>(data, null, 0);
    }

    private PitCursor cursorFor(PitRequest request, String index) {
        if (request.pitId() == null) {
            return new PitCursor(index, PitLifecycle.open(searchClient, index, request.ttl()), 0);
        }
        PitCursor cursor = PitCursor.decode(request.pitId());
        if (!cursor.index().equals(index)) {
            throw new IllegalArgumentException("pitId belongs to a different search");
        }
        return cursor;
    }
}
