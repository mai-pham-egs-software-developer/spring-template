package com.my.craft.opensearch.models.scroll;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import com.my.craft.opensearch.models.pit.PointInTimePaging;
import org.opensearch.client.opensearch._types.Time;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.my.craft.opensearch.SearchClient;
import com.my.craft.opensearch.exception.OpenSearchOperationException;
import com.my.craft.opensearch.models.ScrollRequest;
import com.my.craft.opensearch.models.ScrollResponse;
import com.my.craft.opensearch.models.Sort;

/**
 * The scroll paging strategy shared by every {@link ScrollRequest}-based search -- the
 * counterpart of {@link PointInTimePaging}:
 *
 * <ul>
 *   <li>First page runs the query with {@code scroll=ttl}; OpenSearch keeps a search context (a
 *       snapshot plus the read position) server-side, so later pages don't shift when data changes
 *       and aren't limited by {@code index.max_result_window}.
 *   <li>Next pages call the scroll API with the scroll id -- query, sort and page size are fixed at
 *       the first page and can't change.
 *   <li>The last page is detected against the exact {@code hits.total} ({@code track_total_hits});
 *       the scroll context is then cleared and the cursor is {@code null}.
 * </ul>
 *
 * <p>Limits compared with point in time: OpenSearch rejects {@code collapse} in a scroll context
 * ("cannot use `collapse` in a scroll context"), so a scroll can't group chunks per file -- each
 * hit is one document. Scroll is also forward-only (no jumping back or re-reading a page), and
 * every open context holds resources on the cluster until it's cleared or expires.
 */
@Component
public class ScrollPaging {

    private static final Logger log = LoggerFactory.getLogger(ScrollPaging.class);

    private final SearchClient searchClient;

    public ScrollPaging(SearchClient searchClient) {
        this.searchClient = searchClient;
    }

    /**
     * {@code query} (query/highlight -- anything but index, sort, scroll and size; no collapse) and
     * {@code sort} are only used for the first page; later pages continue the scroll context they
     * created.
     */
    public <D, T> ScrollResponse<T> search(
            ScrollRequest request,
            String index,
            Sort sort,
            UnaryOperator<SearchRequest.Builder> query,
            Class<D> documentType,
            Function<Hit<D>, T> mapper) {
        Time keepAlive = Time.of(t -> t.time(request.ttl() + "s"));
        SearchResult<D> result;
        long seenBefore;
        try {
            if (request.scrollId() == null) {
                SearchRequest searchRequest = SearchRequest.of(s -> query.apply(s.index(index)
                        .sort(sort.toSortOptions())
                        .scroll(keepAlive)
                        .size(request.size())
                        .trackTotalHits(th -> th.enabled(true))));
                result = searchClient.raw().search(searchRequest, documentType);
                seenBefore = 0;
            } else {
                ScrollCursor cursor = ScrollCursor.decode(request.scrollId());
                if (!cursor.index().equals(index)) {
                    throw new IllegalArgumentException("scrollId belongs to a different search");
                }
                result = searchClient.raw().scroll(
                        r -> r.scrollId(cursor.openSearchScrollId()).scroll(keepAlive), documentType);
                seenBefore = cursor.seen();
            }
        } catch (IOException e) {
            throw new OpenSearchOperationException("scroll search failed on index " + index, e);
        }

        List<Hit<D>> hits = result.hits().hits();
        long seen = seenBefore + hits.size();
        long total = result.hits().total() != null ? result.hits().total().value() : seen;
        List<T> data = hits.stream().map(mapper).toList();

        if (!hits.isEmpty() && seen < total) {
            String next = new ScrollCursor(index, result.scrollId(), seen).encode();
            return new ScrollResponse<>(data, next, request.ttl());
        }
        clear(result.scrollId());
        return new ScrollResponse<>(data, null, 0);
    }

    /** Best effort: an uncleared scroll context still expires on its own after its keep-alive. */
    private void clear(String openSearchScrollId) {
        if (openSearchScrollId == null) {
            return;
        }
        try {
            searchClient.raw().clearScroll(c -> c.scrollId(openSearchScrollId));
        } catch (IOException | RuntimeException e) {
            log.warn("failed to clear scroll context, it will expire on its own", e);
        }
    }
}
