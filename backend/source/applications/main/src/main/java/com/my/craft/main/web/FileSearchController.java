package com.my.craft.main.web;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.my.craft.main.service.FileSearchAppService;
import com.my.craft.main.service.dto.FileSearchItem;
import com.my.craft.main.service.dto.PitPage;
import com.my.craft.main.service.dto.ScrollPage;

/**
 * File search over {@code file-chunks}, one result per file with {@code {chunk, value}} snippets:
 * {@code /} ranks files by relevance (point-in-time paging, top 3 snippets), {@code /scroll} walks
 * files in {@code fileRefId} order with all their matching chunks (point in time + search_after;
 * the scroll API can't group chunks per file).
 */
@RestController
@RequestMapping("/api/search/files")
public class FileSearchController {

    private final FileSearchAppService fileSearchAppService;

    public FileSearchController(FileSearchAppService fileSearchAppService) {
        this.fileSearchAppService = fileSearchAppService;
    }

    @PostMapping("/create-index")
    public ResponseEntity<Void> createIndex() {
        fileSearchAppService.createIndex();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/seed")
    public ResponseEntity<Void> seed() {
        fileSearchAppService.seedSampleData();
        return ResponseEntity.noContent().build();
    }

    /** Random test set: {@code files} files, each 1..{@code maxChunks} chunks; returns the chunk count. */
    @PostMapping("/seed-random")
    public Map<String, Integer> seedRandom(
            @RequestParam(defaultValue = "100") int files,
            @RequestParam(defaultValue = "20") int maxChunks,
            @RequestParam(defaultValue = "42") long seed) {
        int chunks = fileSearchAppService.seedRandomData(files, maxChunks, seed);
        return Map.of("files", files, "chunks", chunks);
    }

    /**
     * First page: omit {@code pitId}. Next page: send back the response's {@code pitId} with the same
     * {@code q}, until it comes back {@code null}. {@code ttl} is in seconds.
     */
    @GetMapping
    public PitPage<FileSearchItem> search(
            @RequestParam String q,
            @RequestParam(required = false) String pitId,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "300") long ttl) {
        return fileSearchAppService.search(q, pitId, size, ttl);
    }

    /**
     * First page: omit {@code scrollId}. Next page: send back the response's {@code scrollId} until it
     * comes back {@code null}, with the same {@code q}. {@code size} counts files; each file appears
     * on exactly one page.
     */
    @GetMapping("/scroll")
    public ScrollPage<FileSearchItem> scroll(
            @RequestParam String q,
            @RequestParam(required = false) String scrollId,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "300") long ttl) {
        return fileSearchAppService.scroll(q, scrollId, size, ttl);
    }
}
