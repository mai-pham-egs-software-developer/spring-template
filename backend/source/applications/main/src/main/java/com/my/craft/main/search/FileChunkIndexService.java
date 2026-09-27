package com.my.craft.main.search;

import java.util.List;

import org.opensearch.client.opensearch._types.Refresh;
import org.opensearch.client.opensearch.core.search.FieldCollapse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.InnerHits;
import org.springframework.stereotype.Service;

import com.my.craft.main.search.index.FileChunkIndex;
import com.my.craft.main.service.dto.FileSearchItem;
import com.my.craft.main.service.dto.FileSnippet;
import com.my.craft.opensearch.SearchClient;
import com.my.craft.opensearch.models.KeywordSearchCriteria;
import com.my.craft.opensearch.models.PitRequest;
import com.my.craft.opensearch.models.PitResponse;
import com.my.craft.opensearch.models.Sort;
import com.my.craft.opensearch.models.pit.PointInTimePaging;
import com.my.craft.opensearch.models.pit.SearchAfterPaging;
import com.my.craft.opensearch.models.scroll.ScrollPaging;
import com.my.craft.opensearch.service.BaseOpenSearchService;

/**
 * {@code file-chunks}: the generic operations from {@link BaseOpenSearchService} plus two file
 * searches, both one result per file -- {@code subject}/{@code documentType} are denormalized
 * onto every chunk, so no join back to the file record is needed.
 */
@Service
public class FileChunkIndexService extends BaseOpenSearchService<FileChunkIndex> {

    private static final String MATCHES = "matches";
    private static final int MAX_SNIPPETS_PER_FILE = 3;
    /** OpenSearch's default {@code index.max_inner_result_window}: inner_hits can't return more. */
    private static final int MAX_CHUNKS_PER_FILE = 100;

    public FileChunkIndexService(
            SearchClient searchClient,
            PointInTimePaging pitPaging,
            ScrollPaging scrollPaging,
            SearchAfterPaging searchAfterPaging) {
        super(FileChunkIndex.class, searchClient, pitPaging, scrollPaging, searchAfterPaging);
    }

    /**
     * One result per FILE, ranked by relevance: chunks matching {@code keyword} are collapsed on
     * {@code fileRefId}, with up to {@link #MAX_SNIPPETS_PER_FILE} highlighted fragments from its
     * matching chunks.
     */
    public PitResponse<FileSearchItem> searchFiles(String keyword, PitRequest pit) {
        InnerHits matches = InnerHits.of(ih -> ih.name(MATCHES)
                .size(MAX_SNIPPETS_PER_FILE)
                .highlight(h -> h.fields("content", f -> f)));
        FieldCollapse collapse = FieldCollapse.of(c -> c.field("fileRefId").innerHits(matches));
        return searchByPit(
                contentMatch(keyword),
                pit,
                Sort.relevance().withTieBreaker("fileRefId"),
                s -> s.collapse(collapse),
                this::toFileItem);
    }

    /**
     * Infinite scroll over FILES: one result per file with ALL its matching chunks (up to {@link
     * #MAX_CHUNKS_PER_FILE}, in {@code chunkNo} order) as snippets. The scroll API can't collapse, so
     * this is PIT + {@code search_after} collapsed on {@code fileRefId} -- OpenSearch then requires
     * sorting on {@code fileRefId} alone (not relevance). {@code size} counts files and each file
     * appears on exactly one page.
     */
    public PitResponse<FileSearchItem> scrollFiles(String keyword, PitRequest pit) {
        InnerHits matches = InnerHits.of(ih -> ih.name(MATCHES)
                .size(MAX_CHUNKS_PER_FILE)
                .sort(Sort.by("chunkNo", Sort.Direction.ASC).toSortOptions())
                .highlight(h -> h.fields("content", f -> f)));
        FieldCollapse collapse = FieldCollapse.of(c -> c.field("fileRefId").innerHits(matches));
        return searchBySearchAfter(
                contentMatch(keyword),
                pit,
                Sort.by("fileRefId", Sort.Direction.ASC),
                s -> s.collapse(collapse),
                this::toFileItem);
    }

    /**
     * Deletes every chunk whose {@code fileRefId} starts with {@code prefix} -- before re-ingesting a
     * file, so chunks past its new last {@code chunkNo} don't linger. Returns the number deleted.
     */
    public long deleteFilesByPrefix(String prefix) {
        return execute("delete files by prefix", client -> client.deleteByQuery(d -> d.index(index())
                        .query(q -> q.prefix(p -> p.field("fileRefId").value(prefix)))
                        .refresh(Refresh.True)))
                .deleted();
    }

    private static KeywordSearchCriteria contentMatch(String keyword) {
        return KeywordSearchCriteria.of(keyword, "content");
    }

    private FileSearchItem toFileItem(Hit<FileChunkIndex> hit) {
        List<FileSnippet> snippets = hit.innerHits().get(MATCHES).hits().hits().stream()
                .flatMap(inner -> {
                    int chunkNo = inner.source().to(FileChunkIndex.class).getChunkNo();
                    return inner.highlight().getOrDefault("content", List.of()).stream()
                            .map(value -> new FileSnippet(chunkNo, value));
                })
                .toList();
        return toItem(hit.source(), snippets);
    }

    private static FileSearchItem toItem(FileChunkIndex chunk, List<FileSnippet> snippets) {
        return new FileSearchItem(chunk.getFileRefId(), chunk.getSubject(), chunk.getDocumentType(), snippets);
    }
}
