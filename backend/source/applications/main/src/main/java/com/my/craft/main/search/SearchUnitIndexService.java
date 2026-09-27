package com.my.craft.main.search;

import java.util.List;

import org.opensearch.client.opensearch.core.search.FieldCollapse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.InnerHits;
import org.springframework.stereotype.Service;

import com.my.craft.main.search.index.SearchUnitIndex;
import com.my.craft.main.service.dto.SearchType;
import com.my.craft.main.service.dto.UnifiedSearchItem;
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
 * {@code search-units}: PLANs and FILE chunks in one index. {@link #searchUnified} is ONE request:
 * match {@code content} (a PLAN's description or a FILE chunk's text), filter by {@code type},
 * then {@code collapse} on {@code parentRefId} so each PLAN/file appears once -- pages count
 * parents, not chunks -- with each parent's matching chunks as highlighted {@code inner_hits}.
 */
@Service
public class SearchUnitIndexService extends BaseOpenSearchService<SearchUnitIndex> {

    private static final String MATCHES = "matches";
    private static final int MAX_SNIPPETS_PER_PARENT = 3;

    public SearchUnitIndexService(
            SearchClient searchClient,
            PointInTimePaging pitPaging,
            ScrollPaging scrollPaging,
            SearchAfterPaging searchAfterPaging) {
        super(SearchUnitIndex.class, searchClient, pitPaging, scrollPaging, searchAfterPaging);
    }

    /** {@code types} empty/null searches every type. */
    public PitResponse<UnifiedSearchItem> searchUnified(String keyword, List<SearchType> types, PitRequest pit) {
        List<SearchType> effectiveTypes = (types == null || types.isEmpty()) ? List.of(SearchType.values()) : types;
        KeywordSearchCriteria criteria = KeywordSearchCriteria.of(keyword, "content")
                .withFilter("type", effectiveTypes.stream().map(Enum::name).toArray(String[]::new));

        InnerHits matches = InnerHits.of(ih -> ih.name(MATCHES)
                .size(MAX_SNIPPETS_PER_PARENT)
                .highlight(h -> h.fields("content", f -> f)));
        FieldCollapse collapse = FieldCollapse.of(c -> c.field("parentRefId").innerHits(matches));

        return searchByPit(
                criteria, pit, Sort.relevance().withTieBreaker("parentRefId"), s -> s.collapse(collapse), this::toItem);
    }

    private UnifiedSearchItem toItem(Hit<SearchUnitIndex> hit) {
        SearchUnitIndex unit = hit.source();
        List<String> snippets = hit.innerHits().get(MATCHES).hits().hits().stream()
                .flatMap(inner -> inner.highlight().getOrDefault("content", List.of()).stream())
                .toList();
        return new UnifiedSearchItem(unit.getParentRefId(), unit.getType(), unit.getTitle(), snippets);
    }
}
