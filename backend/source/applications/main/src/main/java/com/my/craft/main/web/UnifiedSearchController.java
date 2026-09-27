package com.my.craft.main.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.my.craft.main.service.UnifiedSearchAppService;
import com.my.craft.main.service.dto.PitPage;
import com.my.craft.main.service.dto.SearchType;
import com.my.craft.main.service.dto.UnifiedSearchItem;

/**
 * PLANs and FILE chunks share one {@code search-units} index; one request returns a single ranked
 * list mixing both types, one result per PLAN/file, paged by point in time.
 */
@RestController
@RequestMapping("/api/search/unified")
public class UnifiedSearchController {

    private final UnifiedSearchAppService unifiedSearchAppService;

    public UnifiedSearchController(UnifiedSearchAppService unifiedSearchAppService) {
        this.unifiedSearchAppService = unifiedSearchAppService;
    }

    @PostMapping("/create-index")
    public ResponseEntity<Void> createIndex() {
        unifiedSearchAppService.createIndex();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/seed")
    public ResponseEntity<Void> seed() {
        unifiedSearchAppService.seedSampleData();
        return ResponseEntity.noContent().build();
    }

    /**
     * {@code types} omitted/empty searches every type; pass a comma-separated list to narrow it.
     * First page: omit {@code pitId}. Next page / infinite scroll: send back the response's {@code
     * pitId} with the same {@code q}/{@code types}, until it comes back {@code null}. {@code ttl} is
     * in seconds.
     */
    @GetMapping
    public PitPage<UnifiedSearchItem> search(
            @RequestParam String q,
            @RequestParam(required = false) List<SearchType> types,
            @RequestParam(required = false) String pitId,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "300") long ttl) {
        return unifiedSearchAppService.search(q, types, pitId, size, ttl);
    }
}
