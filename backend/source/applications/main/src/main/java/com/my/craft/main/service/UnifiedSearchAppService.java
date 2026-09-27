package com.my.craft.main.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.my.craft.main.search.SearchUnitIndexService;
import com.my.craft.main.search.index.SearchUnitIndex;
import com.my.craft.main.service.dto.PitPage;
import com.my.craft.main.service.dto.SearchType;
import com.my.craft.main.service.dto.UnifiedSearchItem;
import com.my.craft.opensearch.models.PitRequest;
import com.my.craft.opensearch.models.PitResponse;

/**
 * App side of the unified search: builds the opensearch library's paging request and converts its
 * {@link PitResponse} to this app's DTO, so controllers never depend on the search library.
 */
@Service
public class UnifiedSearchAppService {

    private final SearchUnitIndexService searchUnitIndexService;

    public UnifiedSearchAppService(SearchUnitIndexService searchUnitIndexService) {
        this.searchUnitIndexService = searchUnitIndexService;
    }

    public void createIndex() {
        searchUnitIndexService.registerMapping();
    }

    /** Sample data: 2 plans plus 1 file split into 3 chunks, as search units. */
    public void seedSampleData() {
        String subject = "Drill Press Operator Manual";
        searchUnitIndexService.bulkIngest(List.of(
                SearchUnitIndex.ofPlan(
                        "plan-1", "Drill Press Maintenance Plan", "Quarterly maintenance plan for the workshop drill press"),
                SearchUnitIndex.ofPlan(
                        "plan-2", "Saw Blade Replacement Plan", "Annual blade replacement schedule for all circular saws"),
                SearchUnitIndex.ofFileChunk("file-1", subject, "PDF", 0,
                        "Chapter 1: Introduction. This manual covers safe operation of the workshop drill press."),
                SearchUnitIndex.ofFileChunk("file-1", subject, "PDF", 1,
                        "Chapter 2: Before each use, inspect the drill press chuck and check the belt tension."),
                SearchUnitIndex.ofFileChunk("file-1", subject, "PDF", 2,
                        "Chapter 3: Maintenance schedule for the drill press is quarterly, per the maintenance plan.")));
    }

    /** {@code types} empty/null searches every type. */
    public PitPage<UnifiedSearchItem> search(String keyword, List<SearchType> types, String pitId, int size, long ttl) {
        PitResponse<UnifiedSearchItem> response =
                searchUnitIndexService.searchUnified(keyword, types, new PitRequest(pitId, size, ttl));
        return new PitPage<>(response.data(), response.pitId(), response.remainingDuration());
    }
}
