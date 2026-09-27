package com.my.craft.opensearch.service;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.search.Hit;

import com.my.craft.opensearch.SearchClient;
import com.my.craft.opensearch.exception.OpenSearchBulkException;
import com.my.craft.opensearch.exception.OpenSearchOperationException;
import com.my.craft.opensearch.index.BaseIndexDocument;
import com.my.craft.opensearch.index.IndexMappingFactory;
import com.my.craft.opensearch.models.PageRequest;
import com.my.craft.opensearch.models.PageResponse;
import com.my.craft.opensearch.models.PitRequest;
import com.my.craft.opensearch.models.PitResponse;
import com.my.craft.opensearch.models.ScrollRequest;
import com.my.craft.opensearch.models.ScrollResponse;
import com.my.craft.opensearch.models.SearchCriteria;
import com.my.craft.opensearch.models.Sort;
import com.my.craft.opensearch.models.pit.PointInTimePaging;
import com.my.craft.opensearch.models.pit.SearchAfterPaging;
import com.my.craft.opensearch.models.scroll.ScrollPaging;

/**
 * {@link OpenSearchService} for one {@link BaseIndexDocument} type -- the index name and mapping
 * come from {@code T}'s annotations -- plus the shared building blocks a concrete service needs
 * beyond the basic interface:
 *
 * <ul>
 *   <li><b>Searches</b>: protected {@code searchByPit}/{@code searchByScroll}/{@code searchByPage}
 *       overloads taking a request {@code customizer} (collapse, highlight, aggregations...) and a
 *       {@code mapper} from hit to any result type; the public interface methods are these with no
 *       customization and {@link #toDocument} as the mapper.
 *   <li><b>Conversion</b>: {@link #toDocument} (override to e.g. copy highlights onto the
 *       document), {@link #toDocuments}, {@link #totalOf}, {@link #toIndexOperation}, {@link
 *       #resolveSort}.
 *   <li><b>Execution</b>: {@link #execute} runs a client call and wraps {@link IOException} as
 *       {@link OpenSearchOperationException} with the index name.
 * </ul>
 *
 * A concrete service per index only has to pass its document class:
 *
 * <pre>{@code
 * @Service
 * public class FileChunkIndexService extends BaseOpenSearchService<FileChunkIndex> {
 *     public FileChunkIndexService(SearchClient client, PointInTimePaging pit, ScrollPaging scroll, SearchAfterPaging after) {
 *         super(FileChunkIndex.class, client, pit, scroll, after);
 *     }
 * }
 * }</pre>
 *
 * PIT and page searches append {@link #TIE_BREAKER} ({@code refId}) to the sort so equal values
 * keep a fixed order across pages; documents without a {@code refId} sort last among equals.
 */
public abstract class BaseOpenSearchService<T extends BaseIndexDocument> implements OpenSearchService<T> {

    protected static final String TIE_BREAKER = "refId";

    private final Class<T> documentType;
    private final String index;
    private final SearchClient searchClient;
    private final PointInTimePaging pitPaging;
    private final ScrollPaging scrollPaging;
    private final SearchAfterPaging searchAfterPaging;

    protected BaseOpenSearchService(
            Class<T> documentType,
            SearchClient searchClient,
            PointInTimePaging pitPaging,
            ScrollPaging scrollPaging,
            SearchAfterPaging searchAfterPaging) {
        this.documentType = documentType;
        this.index = IndexMappingFactory.indexNameOf(documentType);
        this.searchClient = searchClient;
        this.pitPaging = pitPaging;
        this.scrollPaging = scrollPaging;
        this.searchAfterPaging = searchAfterPaging;
    }

    public String index() {
        return index;
    }

    public Class<T> documentType() {
        return documentType;
    }

    // ---------------------------------------------------------------- OpenSearchService

    @Override
    public PitResponse<T> searchByPit(SearchCriteria criteria, PitRequest pit, Sort sort) {
        return searchByPit(criteria, pit, sort, UnaryOperator.identity(), this::toDocument);
    }

    @Override
    public ScrollResponse<T> searchByScroll(SearchCriteria criteria, ScrollRequest scroll, Sort sort) {
        return searchByScroll(criteria, scroll, sort, UnaryOperator.identity(), this::toDocument);
    }

    @Override
    public PageResponse<T> searchByPage(SearchCriteria criteria, PageRequest page, Sort sort) {
        return searchByPage(criteria, page, sort, UnaryOperator.identity(), this::toDocument);
    }

    @Override
    public void registerMapping() {
        execute("register mapping", client -> {
            if (!client.indices().exists(e -> e.index(index)).value()) {
                client.indices().create(IndexMappingFactory.createIndexRequest(documentType));
            }
            return null;
        });
    }

    @Override
    public void bulkIngest(List<T> documents) {
        if (documents.isEmpty()) {
            return;
        }
        List<BulkOperation> operations = documents.stream().map(this::toIndexOperation).toList();
        BulkResponse response = execute("bulk ingest " + documents.size() + " document(s)", client -> client.bulk(b -> b.operations(operations)));
        if (response.errors()) {
            throw new OpenSearchBulkException(response);
        }
    }

    // ---------------------------------------------------------------- shared searches

    /** {@code customizer} adds anything but index, pit, sort and paging (collapse, highlight, ...). */
    protected <R> PitResponse<R> searchByPit(
            SearchCriteria criteria,
            PitRequest pit,
            Sort sort,
            UnaryOperator<SearchRequest.Builder> customizer,
            Function<Hit<T>, R> mapper) {
        return pitPaging.search(
                pit,
                index,
                resolveSort(sort).withTieBreaker(TIE_BREAKER),
                s -> customizer.apply(s.query(criteria.toQuery())),
                documentType,
                mapper);
    }

    /**
     * PIT + {@code search_after}: no offset limit and works with {@code collapse}. Unlike {@link
     * #searchByPit}, NO tiebreaker is appended -- {@code sort} must already be total, and with
     * {@code collapse} it must be the collapse field alone.
     */
    protected <R> PitResponse<R> searchBySearchAfter(
            SearchCriteria criteria,
            PitRequest pit,
            Sort sort,
            UnaryOperator<SearchRequest.Builder> customizer,
            Function<Hit<T>, R> mapper) {
        return searchAfterPaging.search(
                pit, index, resolveSort(sort), s -> customizer.apply(s.query(criteria.toQuery())), documentType, mapper);
    }

    /** {@code customizer} adds anything but index, sort, scroll and size -- no collapse in a scroll. */
    protected <R> ScrollResponse<R> searchByScroll(
            SearchCriteria criteria,
            ScrollRequest scroll,
            Sort sort,
            UnaryOperator<SearchRequest.Builder> customizer,
            Function<Hit<T>, R> mapper) {
        return scrollPaging.search(
                scroll, index, resolveSort(sort), s -> customizer.apply(s.query(criteria.toQuery())), documentType, mapper);
    }

    /** {@code customizer} adds anything but index, sort and paging (collapse, highlight, ...). */
    protected <R> PageResponse<R> searchByPage(
            SearchCriteria criteria,
            PageRequest page,
            Sort sort,
            UnaryOperator<SearchRequest.Builder> customizer,
            Function<Hit<T>, R> mapper) {
        SearchRequest request = SearchRequest.of(s -> customizer.apply(s.index(index)
                .query(criteria.toQuery())
                .sort(resolveSort(sort).withTieBreaker(TIE_BREAKER).toSortOptions())
                .from(page.from())
                .size(page.size())
                .trackTotalHits(th -> th.enabled(true))));
        SearchResponse<T> response = execute("page search", client -> client.search(request, documentType));
        List<R> data = response.hits().hits().stream().map(mapper).toList();
        return PageResponse.of(page, totalOf(response), data);
    }

    // ---------------------------------------------------------------- shared conversion

    /** Hit to document; override to enrich it (e.g. copy highlight fragments onto the document). */
    protected T toDocument(Hit<T> hit) {
        return hit.source();
    }

    protected List<T> toDocuments(SearchResponse<T> response) {
        return response.hits().hits().stream().map(this::toDocument).toList();
    }

    /** {@code hits.total}, falling back to the number of returned hits when the total isn't tracked. */
    protected static long totalOf(SearchResponse<?> response) {
        return response.hits().total() != null ? response.hits().total().value() : response.hits().hits().size();
    }

    /** Index operation using {@code refId} as the document id, so re-ingesting replaces instead of duplicating. */
    protected BulkOperation toIndexOperation(T document) {
        return BulkOperation.of(op -> op.index(i -> {
            i.index(index).document(document);
            if (document.getRefId() != null) {
                i.id(document.getRefId());
            }
            return i;
        }));
    }

    /** {@code null}/empty sort means relevance. */
    protected static Sort resolveSort(Sort sort) {
        return sort == null || sort.orders().isEmpty() ? Sort.relevance() : sort;
    }

    // ---------------------------------------------------------------- shared execution

    @FunctionalInterface
    protected interface OpenSearchCall<R> {
        R apply(OpenSearchClient client) throws IOException;
    }

    /** Runs {@code call} against the raw client, wrapping {@link IOException} with {@code action} and the index. */
    protected <R> R execute(String action, OpenSearchCall<R> call) {
        try {
            return call.apply(searchClient.raw());
        } catch (IOException e) {
            throw new OpenSearchOperationException(action + " failed on index " + index, e);
        }
    }
}
