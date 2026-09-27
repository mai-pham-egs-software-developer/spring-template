package com.my.craft.opensearch.service;

import java.util.List;

import com.my.craft.opensearch.index.BaseIndexDocument;
import com.my.craft.opensearch.models.PageRequest;
import com.my.craft.opensearch.models.PageResponse;
import com.my.craft.opensearch.models.PitRequest;
import com.my.craft.opensearch.models.PitResponse;
import com.my.craft.opensearch.models.ScrollRequest;
import com.my.craft.opensearch.models.ScrollResponse;
import com.my.craft.opensearch.models.SearchCriteria;
import com.my.craft.opensearch.models.Sort;

/**
 * Basic operations on the one index that stores {@code T} ({@link
 * com.my.craft.opensearch.index.Document @Document} on {@code T}). The three searches take the
 * same criteria and sort and differ only in paging strategy:
 *
 * <ul>
 *   <li>{@link #searchByPit}: cursor over a frozen snapshot -- no duplicates/skips while data
 *       changes, forward only, up to 10,000 results.
 *   <li>{@link #searchByScroll}: server-side scroll context -- no result-window limit, forward
 *       only, criteria/sort/size fixed by the first page; for exports rather than UI paging.
 *   <li>{@link #searchByPage}: plain page number with a total -- random access to any page, but
 *       each page reads the live index, so results can shift between requests.
 * </ul>
 *
 * A {@code null} sort means relevance ({@code _score} descending).
 */
public interface OpenSearchService<T extends BaseIndexDocument> {

    PitResponse<T> searchByPit(SearchCriteria criteria, PitRequest pit, Sort sort);

    ScrollResponse<T> searchByScroll(SearchCriteria criteria, ScrollRequest scroll, Sort sort);

    PageResponse<T> searchByPage(SearchCriteria criteria, PageRequest page, Sort sort);

    /** Creates the index with a mapping built from {@code T}'s {@code @Field}s; no-op if it already exists. */
    void registerMapping();

    /**
     * Indexes {@code documents} in one bulk request, using each document's {@code refId} as the
     * document id when set -- re-ingesting the same {@code refId} replaces it instead of duplicating.
     */
    void bulkIngest(List<T> documents);
}
